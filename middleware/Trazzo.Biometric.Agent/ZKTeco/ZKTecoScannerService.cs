using System.Diagnostics;
using System.Globalization;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Services;
using Trazzo.Biometric.Agent.Utilities;

namespace Trazzo.Biometric.Agent.ZKTeco;

public sealed class ZKTecoScannerService(
    IZKTecoNativeSdk sdk,
    ICryptographyService cryptoService,
    IConfiguration configuration,
    ILogger<ZKTecoScannerService> logger) : IBiometricScannerService
{
    private const string DeviceDisconnectedMessage = "Lector biométrico desconectado.";
    private const string NoDeviceConnectedMessage = "No se encontró ningún lector biométrico conectado.";

    // ZK9500: 300×400 px (FAP20) según ZKTeco Fingerprint Scanners Hardware Selection Guide
    private const int DefaultImageWidth = 300;
    private const int DefaultImageHeight = 400;
    private const int DefaultTemplateBufferSize = 2048;
    private const double DefaultCaptureTimeoutSeconds = 2.5;
    private const int DefaultCapturePollingIntervalMilliseconds = 50;
    private const int DefaultPostCaptureCooldownMilliseconds = 300;
    private const int DefaultMinimumTemplateSize = 400;
    private const int DefaultEnrollmentSamples = 3;
    private const double DefaultEnrollmentSampleTimeoutSeconds = 2.5;
    private const double MaxCaptureTimeoutSeconds = 2.5;
    private const int MaxCapturePollingIntervalMilliseconds = 80;
    private const int MaxPostCaptureCooldownMilliseconds = 300;
    private const double MaxEnrollmentSampleTimeoutSeconds = 2.5;
    private const double DefaultMinimumForegroundCoveragePercent = 8;
    private const double DefaultMaximumForegroundCoveragePercent = 75;
    private const double DefaultMinimumContrastScore = 18;
    private const double DefaultCenterTolerancePercent = 28;
    private const double DefaultContrastThresholdOffset = 15;

    private readonly SemaphoreSlim _sdkLock = new(1, 1);
    private readonly SemaphoreSlim _operationLock = new(1, 1);
    private readonly object _operationStateLock = new();
    private readonly double _captureTimeoutSeconds = GetStandardizedDoubleConfigurationValue(configuration, "Biometric:CaptureTimeoutSeconds", DefaultCaptureTimeoutSeconds, MaxCaptureTimeoutSeconds);
    private readonly int _capturePollingIntervalMilliseconds = GetStandardizedConfigurationValue(configuration, "Biometric:CapturePollingIntervalMilliseconds", DefaultCapturePollingIntervalMilliseconds, MaxCapturePollingIntervalMilliseconds);
    private readonly int _postCaptureCooldownMilliseconds = GetStandardizedConfigurationValue(configuration, "Biometric:PostCaptureCooldownMilliseconds", DefaultPostCaptureCooldownMilliseconds, MaxPostCaptureCooldownMilliseconds);
    private readonly int _templateBufferSize = GetPositiveConfigurationValue(configuration, "Biometric:TemplateBufferSize", DefaultTemplateBufferSize);
    private readonly bool _includeFingerprintImageInResponses = configuration.GetValue("Biometric:IncludeFingerprintImageInResponses", false);
    private readonly int _minimumTemplateSize = GetPositiveConfigurationValue(configuration, "Biometric:Quality:MinimumTemplateSize", DefaultMinimumTemplateSize);
    private readonly int _enrollmentSamples = ClampEnrollmentSamples(GetPositiveConfigurationValue(configuration, "Enrollment:RequiredSamples", DefaultEnrollmentSamples), logger);
    private readonly double _enrollmentSampleTimeoutSeconds = GetStandardizedDoubleConfigurationValue(configuration, "Enrollment:SampleTimeoutSeconds", DefaultEnrollmentSampleTimeoutSeconds, MaxEnrollmentSampleTimeoutSeconds);
    private readonly bool _requireFingerLiftBetweenEnrollmentSamples = configuration.GetValue("Enrollment:RequireFingerLiftBetweenSamples", true);
    private readonly FingerprintQualityCriteria _qualityCriteria = new(
        GetPositiveDoubleConfigurationValue(configuration, "Biometric:Quality:MinimumForegroundCoveragePercent", DefaultMinimumForegroundCoveragePercent),
        GetPositiveDoubleConfigurationValue(configuration, "Biometric:Quality:MaximumForegroundCoveragePercent", DefaultMaximumForegroundCoveragePercent),
        GetPositiveDoubleConfigurationValue(configuration, "Biometric:Quality:MinimumContrastScore", DefaultMinimumContrastScore),
        configuration.GetValue("Biometric:Quality:RequireCenteredFingerprint", true),
        GetPositiveDoubleConfigurationValue(configuration, "Biometric:Quality:CenterTolerancePercent", DefaultCenterTolerancePercent),
        GetPositiveDoubleConfigurationValue(configuration, "Biometric:Quality:ContrastThresholdOffset", DefaultContrastThresholdOffset));

    private byte[] _imageBuffer = new byte[DefaultImageWidth * DefaultImageHeight];
    private byte[] _templateBuffer = new byte[DefaultTemplateBufferSize];
    private bool _initialized;
    private int _imageWidth = DefaultImageWidth;
    private int _imageHeight = DefaultImageHeight;
    private int _imageDataSize;
    private IntPtr _databaseHandle = IntPtr.Zero;
    private IntPtr _deviceHandle = IntPtr.Zero;
    private string? _deviceSerial;
    private Guid? _activeOperationId;
    private CancellationTokenSource? _activeOperationCts;
    private BiometricOperationState _operationState = BiometricOperationState.Idle;

    public async Task InitializeAsync(CancellationToken cancellationToken)
    {
        await _sdkLock.WaitAsync(cancellationToken);

        try
        {
            if (_initialized)
            {
                return;
            }

            if (!sdk.IsAvailable)
            {
                logger.LogWarning("SDK ZKTeco no disponible: {LoadError}", sdk.LoadError);
                return;
            }

            int initResult = sdk.Init();
            if (initResult != 0)
            {
                logger.LogWarning("La inicialización del SDK ZKTeco falló: {Message}", ZKTecoErrorMapper.ToMessage(initResult));
                return;
            }

            _initialized = true;
            logger.LogInformation("SDK inicializado.");

            if (!OpenFirstDevice())
            {
                return;
            }

            InitializeDatabase();
            ConfigureCaptureBuffers();
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error al inicializar el servicio del lector ZKTeco.");
        }
        finally
        {
            _sdkLock.Release();
        }
    }

    public async Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken)
    {
        await _sdkLock.WaitAsync(cancellationToken);

        try
        {
            return GetLiveDeviceStatus();
        }
        finally
        {
            _sdkLock.Release();
        }
    }

    public async Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken)
    {
        if (!await TryEnterOperationAsync())
        {
            return FingerprintCaptureResult.Failed(BusyMessage);
        }

        Guid? operationId = null;

        try
        {
            FingerprintCaptureResult? readinessFailure = await EnsureReadyForOperationAsync(cancellationToken);
            if (readinessFailure is not null)
            {
                return readinessFailure;
            }

            operationId = StartOperation(BiometricOperationState.Capturing, cancellationToken);
            CapturedSample sample = await CaptureValidSampleAsync(operationId.Value, _captureTimeoutSeconds);

            if (!sample.Success)
            {
                FinalizeOperation(operationId.Value, sample.FinalState);
                return FingerprintCaptureResult.Failed(sample.Message, sample.Quality, sample.Image?.Base64, sample.Image?.MimeType, sample.Image?.DataUrl);
            }

            logger.LogInformation("Captura finalizada correctamente. Cerrando sesión: {OperationId}", operationId);
            FinalizeOperation(operationId.Value, BiometricOperationState.Completed);

            EncryptedPayload? encryptedTemplate = cryptoService.TryEncryptTemplate(sample.Template, sample.TemplateSize);

            return FingerprintCaptureResult.Succeeded(
                sample.Template,
                sample.TemplateSize,
                new FingerprintCaptureOptions(
                    DeviceId: _deviceSerial,
                    EncryptedTemplate: encryptedTemplate,
                    Quality: sample.Quality,
                    FingerprintImageBase64: sample.Image?.Base64,
                    FingerprintImageMimeType: sample.Image?.MimeType,
                    FingerprintImageDataUrl: sample.Image?.DataUrl));
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "La captura falló.");
            if (operationId is Guid id)
            {
                FinalizeOperation(id, BiometricOperationState.Error);
            }

            return FingerprintCaptureResult.Failed($"La captura falló: {ex.Message}");
        }
        catch (OperationCanceledException)
        {
            if (operationId is Guid id)
            {
                FinalizeOperation(id, BiometricOperationState.Error);
            }

            return FingerprintCaptureResult.Failed("Lectura ignorada porque la sesión de captura ya finalizó.");
        }
        finally
        {
            _operationLock.Release();
        }
    }

    public async Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken)
    {
        if (!await TryEnterOperationAsync())
        {
            return FingerprintIdentifyResult.Failed(BusyMessage);
        }

        Guid? operationId = null;

        try
        {
            FingerprintCaptureResult? readinessFailure = await EnsureReadyForOperationAsync(cancellationToken);
            if (readinessFailure is not null)
            {
                return FingerprintIdentifyResult.Failed(readinessFailure.Message, readinessFailure.Quality);
            }

            operationId = StartOperation(BiometricOperationState.Identifying, cancellationToken);
            CapturedSample sample = await CaptureValidSampleAsync(operationId.Value, _captureTimeoutSeconds);

            if (!sample.Success)
            {
                FinalizeOperation(operationId.Value, sample.FinalState);
                return FingerprintIdentifyResult.Failed("No se pudo capturar la huella para identificación.", sample.Quality);
            }

            logger.LogInformation("Captura finalizada correctamente. Cerrando sesión: {OperationId}", operationId);
            FinalizeOperation(operationId.Value, BiometricOperationState.Completed);

            EncryptedPayload? encryptedIdentifyTemplate = cryptoService.TryEncryptTemplate(sample.Template, sample.TemplateSize);

            return FingerprintIdentifyResult.Succeeded(
                sample.Template,
                sample.TemplateSize,
                sample.Quality,
                deviceId: _deviceSerial,
                encryptedTemplate: encryptedIdentifyTemplate);
        }
        catch (Exception ex) when (ex is not OperationCanceledException)
        {
            logger.LogError(ex, "La identificación falló.");
            if (operationId is Guid id)
            {
                FinalizeOperation(id, BiometricOperationState.Error);
            }

            return FingerprintIdentifyResult.Failed("No se pudo capturar la huella para identificación.");
        }
        catch (OperationCanceledException)
        {
            if (operationId is Guid id)
            {
                FinalizeOperation(id, BiometricOperationState.Error);
            }

            return FingerprintIdentifyResult.Failed("Lectura ignorada porque la sesión de captura ya finalizó.");
        }
        finally
        {
            _operationLock.Release();
        }
    }

    public async Task<FingerprintEnrollResult> EnrollFingerprintAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken cancellationToken)
    {
        if (!await TryEnterOperationAsync())
        {
            return FingerprintEnrollResult.Failed(BusyMessage);
        }

        Guid? operationId = null;
        List<byte[]> samples = [];

        try
        {
            FingerprintCaptureResult? readinessFailure = await EnsureReadyForOperationAsync(cancellationToken);
            if (readinessFailure is not null)
            {
                return FingerprintEnrollResult.Failed(readinessFailure.Message);
            }

            operationId = StartOperation(BiometricOperationState.Enrolling, cancellationToken);
            CancellationToken operationToken = GetActiveOperationToken(operationId.Value);
            FingerprintEnrollResult? captureFailure = await CaptureEnrollmentSamplesAsync(
                operationId.Value,
                samples,
                progressCallback,
                operationToken);
            if (captureFailure is not null)
                return captureFailure;

            byte[] registeredTemplate = new byte[_templateBufferSize];
            int registeredTemplateSize = registeredTemplate.Length;
            int mergeResult = sdk.DBMerge(_databaseHandle, samples[0], samples[1], samples[2], registeredTemplate, ref registeredTemplateSize);
            if (mergeResult != 0 || registeredTemplateSize <= 0)
            {
                logger.LogWarning("DBMerge falló: {Message}", ZKTecoErrorMapper.ToMessage(mergeResult));
                FinalizeOperation(operationId.Value, BiometricOperationState.Error);
                return FingerprintEnrollResult.Failed("No se pudo enrolar la huella. Intente nuevamente.", samples.Count);
            }

            logger.LogInformation("Captura finalizada correctamente. Cerrando sesión: {OperationId}", operationId);
            FinalizeOperation(operationId.Value, BiometricOperationState.Completed);

            EncryptedPayload? encryptedEnrollTemplate = cryptoService.TryEncryptTemplate(registeredTemplate, registeredTemplateSize);

            return FingerprintEnrollResult.Succeeded(
                registeredTemplate,
                registeredTemplateSize,
                samples.Count,
                deviceId: _deviceSerial,
                encryptedTemplate: encryptedEnrollTemplate);
        }
        catch (OperationCanceledException)
        {
            if (operationId is Guid id)
            {
                FinalizeOperation(id, BiometricOperationState.Cancelled);
            }

            return FingerprintEnrollResult.Failed("Enrolamiento cancelado.", samples.Count);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "El enrolamiento falló.");
            if (operationId is Guid id)
            {
                FinalizeOperation(id, BiometricOperationState.Error);
            }

            return FingerprintEnrollResult.Failed("No se pudo enrolar la huella. Intente nuevamente.", samples.Count);
        }
        finally
        {
            _operationLock.Release();
        }
    }

    private async Task<FingerprintEnrollResult?> CaptureEnrollmentSamplesAsync(
        Guid operationId,
        List<byte[]> samples,
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken operationToken)
    {
        for (int step = 1; step <= _enrollmentSamples;)
        {
            await progressCallback(
                FingerprintEnrollProgress.Create(step, _enrollmentSamples, GetPlaceFingerMessage(step)),
                operationToken);
            CapturedSample sample = await CaptureValidSampleAsync(operationId, _enrollmentSampleTimeoutSeconds);

            if (!IsOperationActive(operationId))
                return FingerprintEnrollResult.Failed("Enrolamiento cancelado.", samples.Count);

            if (!sample.Success)
            {
                if (sample.FinalState is BiometricOperationState.TimedOut or BiometricOperationState.Error)
                {
                    FinalizeOperation(operationId, sample.FinalState);
                    return FingerprintEnrollResult.Failed("No se pudo enrolar la huella. Intente nuevamente.", samples.Count);
                }

                await progressCallback(
                    FingerprintEnrollProgress.Create(step, _enrollmentSamples, sample.Message),
                    operationToken);
                continue;
            }

            samples.Add(sample.Template);
            if (step < _enrollmentSamples && _requireFingerLiftBetweenEnrollmentSamples)
            {
                await progressCallback(
                    FingerprintEnrollProgress.Create(step, _enrollmentSamples, "Retire el dedo del lector."),
                    operationToken);
                await WaitForFingerLiftAsync(operationToken);
            }

            step++;
        }

        return null;
    }

    public FingerprintEnrollResult CancelEnrollment()
    {
        lock (_operationStateLock)
        {
            if (_operationState != BiometricOperationState.Enrolling || _activeOperationId is null)
            {
                return FingerprintEnrollResult.Failed("No hay un enrolamiento activo.");
            }

            _activeOperationCts?.Cancel();
        }

        return FingerprintEnrollResult.Failed("Enrolamiento cancelado.");
    }

    private async Task<CapturedSample> CaptureValidSampleAsync(Guid operationId, double timeoutSeconds)
    {
        CancellationToken cancellationToken = GetActiveOperationToken(operationId);
        TimeSpan timeout = TimeSpan.FromSeconds(timeoutSeconds);
        TimeSpan pollingInterval = TimeSpan.FromMilliseconds(_capturePollingIntervalMilliseconds);
        Stopwatch stopwatch = Stopwatch.StartNew();

        logger.LogInformation(
            "Esperando huella. Intervalo: {CapturePollingIntervalMilliseconds} ms. Tiempo máximo: {TimeoutSeconds} segundos.",
            _capturePollingIntervalMilliseconds,
            timeoutSeconds);

        while (true)
        {
            TimeSpan remaining = timeout - stopwatch.Elapsed;
            if (remaining <= TimeSpan.Zero)
                break;

            if (IsDeviceDisconnected())
            {
                logger.LogWarning("El lector fue desconectado durante la captura.");
                CloseCurrentDeviceHandle();
                return CapturedSample.Failed("El lector biométrico fue desconectado durante la captura.", BiometricOperationState.Error);
            }

            // Buffers locales por llamada: evitan race conditions si el SDK bloquea más
            // de nuestro timeout y el task queda corriendo en background tras el WhenAny.
            // _sdkLock dentro del task serializa con DrainResidualReadingsAsync.
            byte[] localImage = new byte[_imageBuffer.Length];
            byte[] localTemplate = new byte[_templateBuffer.Length];

            Task<(int CaptureResult, int TemplateSize)> captureTask = Task.Run(async () =>
            {
                await _sdkLock.WaitAsync(CancellationToken.None);
                try
                {
                    Array.Clear(localImage);
                    Array.Clear(localTemplate);
                    int size = localTemplate.Length;
                    int r = sdk.AcquireFingerprint(_deviceHandle, localImage, localTemplate, ref size);
                    return (r, size);
                }
                finally
                {
                    _sdkLock.Release();
                }
            }, CancellationToken.None);

            bool captureCompleted =
                await Task.WhenAny(captureTask, Task.Delay(remaining, cancellationToken)) == captureTask;

            if (!captureCompleted || cancellationToken.IsCancellationRequested)
                break;

            (int captureResult, int templateSize) = await captureTask;

            Buffer.BlockCopy(localImage, 0, _imageBuffer, 0, Math.Min(localImage.Length, _imageBuffer.Length));
            Buffer.BlockCopy(localTemplate, 0, _templateBuffer, 0, Math.Min(localTemplate.Length, _templateBuffer.Length));

            logger.LogInformation("AcquireFingerprint devolvió: {CaptureResult}", captureResult);

            CapturedSample? detected = TryProcessCaptureResult(captureResult, templateSize, operationId, stopwatch.ElapsedMilliseconds);
            if (detected is not null)
                return detected;

            if (IsFatalCaptureError(captureResult))
            {
                string message = ZKTecoErrorMapper.ToMessage(captureResult);
                logger.LogWarning("La captura falló por un error grave del SDK: {Message}", message);
                return CapturedSample.Failed(message, BiometricOperationState.Error);
            }

            remaining = timeout - stopwatch.Elapsed;
            if (remaining <= TimeSpan.Zero)
                break;
            TimeSpan delay = TimeSpan.FromMilliseconds(Math.Min(remaining.TotalMilliseconds, pollingInterval.TotalMilliseconds));
            await Task.Delay(delay, cancellationToken);
        }

        logger.LogWarning(
            "Tiempo máximo de captura agotado después de {ElapsedMilliseconds} ms. Cerrando sesión: {SessionId}",
            stopwatch.ElapsedMilliseconds,
            operationId);
        return CapturedSample.Failed("Tiempo de espera agotado. Coloque el dedo en el lector.", BiometricOperationState.TimedOut);
    }

    private CapturedSample? TryProcessCaptureResult(int captureResult, int templateSize, Guid operationId, long elapsedMilliseconds)
    {
        if (captureResult != 0 || templateSize <= 0)
            return null;

        int bufferSize = _templateBuffer.Length;
        if (templateSize > bufferSize)
        {
            logger.LogWarning(
                "SDK reportó templateSize={TemplateSize} mayor que el buffer ({BufferSize}). Captura rechazada.",
                templateSize, bufferSize);
            return CapturedSample.Failed("Error interno: tamaño de plantilla excede el buffer.", BiometricOperationState.Error);
        }

        if (!IsOperationActive(operationId))
        {
            logger.LogWarning("Lectura ignorada porque la sesión ya no está activa.");
            return CapturedSample.Failed("Lectura ignorada porque la sesión de captura ya finalizó.", BiometricOperationState.Error);
        }

        logger.LogInformation(
            "Huella detectada para la sesión {OperationId} en {ElapsedMilliseconds} ms. Tamaño de plantilla: {TemplateSize}",
            operationId,
            elapsedMilliseconds,
            templateSize);

        return ValidateCapturedSample(templateSize);
    }

    private CapturedSample ValidateCapturedSample(int templateSize)
    {
        FingerprintImagePayload? image = CreateDebugImagePayload();

        if (templateSize < _minimumTemplateSize)
        {
            FingerprintQualityResult quality = new(false, 0, 0, 0, false, "Plantilla biométrica demasiado pequeña.");
            int minimumTemplateSize = _minimumTemplateSize;
            logger.LogWarning(
                "Captura rechazada por calidad: plantilla demasiado pequeña. Tamaño: {TemplateSize}. Mínimo: {MinimumTemplateSize}.",
                templateSize,
                minimumTemplateSize);
            return CapturedSample.Failed(
                "Huella incompleta o mal posicionada. Coloque el dedo completo y centrado sobre el lector.",
                BiometricOperationState.Rejected,
                quality,
                image);
        }

        FingerprintQualityResult qualityResult = FingerprintQualityAnalyzer.Analyze(_imageBuffer, _imageWidth, _imageHeight, _qualityCriteria);
        logger.LogInformation(
            "Calidad de huella. Aceptable: {IsAcceptable}. Score: {ScorePercent}%. Área cruda: {CoveragePercent}%. Contraste: {ContrastScore}. Centrada: {IsCentered}. Mensaje: {Message}",
            qualityResult.IsAcceptable,
            qualityResult.ScorePercent,
            qualityResult.ForegroundCoveragePercent,
            qualityResult.ContrastScore,
            qualityResult.IsCentered,
            qualityResult.Message);

        if (!qualityResult.IsAcceptable)
        {
            logger.LogWarning("Captura rechazada por calidad.");
            return CapturedSample.Failed(
                "Huella incompleta o mal posicionada. Coloque el dedo completo y centrado sobre el lector.",
                BiometricOperationState.Rejected,
                qualityResult,
                image);
        }

        byte[] template = _templateBuffer[..templateSize].ToArray();
        return CapturedSample.Succeeded(template, templateSize, qualityResult, image);
    }

    private async Task<bool> TryEnterOperationAsync()
    {
        if (!await _operationLock.WaitAsync(0))
        {
            logger.LogWarning("Captura bloqueada: ya hay una sesión activa.");
            return false;
        }

        return true;
    }

    private const string BusyMessage = "Ya hay una operación biométrica en progreso.";

    private async Task<FingerprintCaptureResult?> EnsureReadyForOperationAsync(CancellationToken cancellationToken)
    {
        string? unavailableMessage = GetOperationUnavailableMessage();
        if (unavailableMessage is not null)
        {
            logger.LogWarning("Captura bloqueada: ya hay una sesión activa.");
            return FingerprintCaptureResult.Failed(unavailableMessage);
        }

        await _sdkLock.WaitAsync(cancellationToken);
        try
        {
            unavailableMessage = GetOperationUnavailableMessage();
            if (unavailableMessage is not null)
            {
                return FingerprintCaptureResult.Failed(unavailableMessage);
            }
            FingerprintDeviceStatus liveStatus = GetLiveDeviceStatus();
            if (!liveStatus.IsConnected)
            {
                logger.LogWarning("No se puede capturar porque el lector está desconectado.");
                return FingerprintCaptureResult.Failed(NoDeviceConnectedMessage);
            }

            return null;
        }
        finally
        {
            _sdkLock.Release();
        }
    }

    private string? GetOperationUnavailableMessage()
    {
        lock (_operationStateLock)
        {
            if (_operationState == BiometricOperationState.Idle && _activeOperationId is null)
            {
                return null;
            }

            return "Ya hay una operación biométrica en progreso.";
        }
    }

    private Guid StartOperation(BiometricOperationState operationState, CancellationToken cancellationToken)
    {
        Guid operationId = Guid.NewGuid();

        lock (_operationStateLock)
        {
            _activeOperationCts?.Dispose();
            _activeOperationCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            _activeOperationId = operationId;
            _operationState = operationState;
        }

        logger.LogInformation("Nueva sesión de captura iniciada: {SessionId}", operationId);
        return operationId;
    }

    private CancellationToken GetActiveOperationToken(Guid operationId)
    {
        lock (_operationStateLock)
        {
            if (_activeOperationId != operationId || _activeOperationCts is null)
            {
                throw new OperationCanceledException("La sesión de captura ya finalizó.");
            }

            return _activeOperationCts.Token;
        }
    }

    private bool IsOperationActive(Guid operationId)
    {
        lock (_operationStateLock)
        {
            return _activeOperationId == operationId
                && _activeOperationCts is not null
                && !_activeOperationCts.IsCancellationRequested
                && _operationState is BiometricOperationState.Capturing or BiometricOperationState.Identifying or BiometricOperationState.Enrolling;
        }
    }

    private void FinalizeOperation(Guid operationId, BiometricOperationState finalState)
    {
        CancellationTokenSource? ctsToDispose = null;

        lock (_operationStateLock)
        {
            if (_activeOperationId != operationId)
            {
                return;
            }

            _operationState = BiometricOperationState.Idle;
            ctsToDispose = _activeOperationCts;
            _activeOperationCts = null;
            _activeOperationId = null;
        }

        ctsToDispose?.Cancel();
        ctsToDispose?.Dispose();
        if (logger.IsEnabled(LogLevel.Information))
            logger.LogInformation("Operacion biometrica finalizada con estado {FinalState}. Estado disponible para nueva solicitud.", finalState);
        CompletePostCaptureDrainAsync().ContinueWith(
            t => logger.LogWarning(t.Exception?.GetBaseException(), "Error no controlado durante drenado post-captura."),
            CancellationToken.None,
            TaskContinuationOptions.OnlyOnFaulted,
            TaskScheduler.Default);
    }

    private async Task CompletePostCaptureDrainAsync()
    {
        try
        {
            await DrainResidualReadingsAsync();
            await Task.Delay(_postCaptureCooldownMilliseconds);

            lock (_operationStateLock)
            {
                if (_activeOperationId is null)
                {
                    _operationState = BiometricOperationState.Idle;
                }
            }

            logger.LogInformation("Drenado post-captura finalizado. Estado: Idle.");
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "No se pudo completar el drenado post-captura.");
        }
    }

    private async Task DrainResidualReadingsAsync()
    {
        logger.LogInformation("Drenando lecturas residuales...");

        TimeSpan drainDuration = TimeSpan.FromMilliseconds(Math.Clamp(_postCaptureCooldownMilliseconds, 300, 700));
        Stopwatch stopwatch = Stopwatch.StartNew();

        await _sdkLock.WaitAsync();
        try
        {
            if (IsBiometricOperationInProgress())
            {
                logger.LogDebug("Drenado post-captura omitido porque ya hay una operación activa.");
                return;
            }

            byte[] drainImageBuffer = new byte[_imageBuffer.Length];
            byte[] drainTemplateBuffer = new byte[_templateBufferSize];

            while (stopwatch.Elapsed < drainDuration && _deviceHandle != IntPtr.Zero)
            {
                if (IsBiometricOperationInProgress())
                {
                    logger.LogDebug("Drenado post-captura detenido porque inició una nueva operación.");
                    return;
                }

                Array.Clear(drainImageBuffer);
                Array.Clear(drainTemplateBuffer);
                int templateSize = drainTemplateBuffer.Length;
                int captureResult = await Task.Run(
                    () => sdk.AcquireFingerprint(_deviceHandle, drainImageBuffer, drainTemplateBuffer, ref templateSize));

                if (captureResult == 0 && templateSize > 0)
                {
                    logger.LogInformation("Lectura residual ignorada durante drenaje.");
                }

                await Task.Delay(TimeSpan.FromMilliseconds(Math.Min(_capturePollingIntervalMilliseconds, 100)));
            }
        }
        catch (Exception ex)
        {
            logger.LogDebug(ex, "Error no crítico durante drenaje de lecturas residuales.");
        }
        finally
        {
            _sdkLock.Release();
        }
    }

    private async Task<bool> IsFingerAlreadyOnReaderAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("Esperando retiro del dedo antes de una nueva captura.");

        TimeSpan checkDuration = TimeSpan.FromMilliseconds(Math.Min(_postCaptureCooldownMilliseconds, 700));
        if (checkDuration <= TimeSpan.Zero)
        {
            checkDuration = TimeSpan.FromMilliseconds(300);
        }

        Stopwatch stopwatch = Stopwatch.StartNew();

        while (stopwatch.Elapsed < checkDuration)
        {
            Array.Clear(_imageBuffer);
            Array.Clear(_templateBuffer);
            int templateSize = _templateBuffer.Length;
            int captureResult = await Task.Run(
                () => sdk.AcquireFingerprint(_deviceHandle, _imageBuffer, _templateBuffer, ref templateSize),
                cancellationToken);

            if (captureResult == 0 && templateSize > 0)
            {
                return true;
            }

            await Task.Delay(TimeSpan.FromMilliseconds(Math.Min(_capturePollingIntervalMilliseconds, 100)), cancellationToken);
        }

        return false;
    }

    private async Task WaitForFingerLiftAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("Esperando retiro del dedo antes de una nueva captura.");

        while (!cancellationToken.IsCancellationRequested)
        {
            if (!await IsFingerAlreadyOnReaderAsync(cancellationToken))
            {
                return;
            }

            await Task.Delay(_capturePollingIntervalMilliseconds, cancellationToken);
        }
    }

    private FingerprintImagePayload? CreateDebugImagePayload()
    {
        if (!_includeFingerprintImageInResponses)
        {
            return null;
        }

        try
        {
            FingerprintImagePayload image = FingerprintImageConverter.ConvertGrayscaleRawToPng(_imageBuffer, _imageWidth, _imageHeight);
            int pngSize = image.PngBytes.Length;
            logger.LogInformation("Imagen de huella convertida a PNG para respuesta local de modo de prueba. Tamaño PNG: {PngSize} bytes.", pngSize);
            return image;
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "La plantilla de huella fue capturada, pero la imagen de modo de prueba no se pudo convertir.");
            return null;
        }
    }

    public async ValueTask DisposeAsync()
    {
        await _sdkLock.WaitAsync();

        try
        {
            if (_activeOperationId is Guid operationId)
            {
                FinalizeOperation(operationId, BiometricOperationState.Error);
            }

            if (_deviceHandle != IntPtr.Zero)
            {
                logger.LogInformation("Cerrando lector biométrico ZKTeco.");
                sdk.CloseDevice(_deviceHandle);
                _deviceHandle = IntPtr.Zero;
            }

            if (_databaseHandle != IntPtr.Zero)
            {
                logger.LogInformation("Liberando base biométrica ZKTeco.");
                sdk.DBFree(_databaseHandle);
                _databaseHandle = IntPtr.Zero;
            }

            if (_initialized)
            {
                logger.LogInformation("Finalizando SDK ZKTeco.");
                sdk.Terminate();
                _initialized = false;
            }
        }
        finally
        {
            _sdkLock.Release();
            _sdkLock.Dispose();
            _operationLock.Dispose();
        }
    }

    private bool OpenFirstDevice()
    {
        int deviceCount = sdk.GetDeviceCount();
        if (deviceCount <= 0)
        {
            logger.LogWarning("No se encontró ningún lector biométrico ZKTeco.");
            return false;
        }

        logger.LogInformation("Cantidad de lectores: {DeviceCount}.", deviceCount);

        return TryOpenDevice();
    }

    private bool TryOpenDevice()
    {
        logger.LogInformation("Intentando reabrir lector biométrico...");

        _deviceHandle = sdk.OpenDevice(0);
        if (_deviceHandle == IntPtr.Zero)
        {
            logger.LogWarning("No se pudo abrir el lector biométrico ZKTeco en el índice 0.");
            _deviceSerial = null;
            return false;
        }

        // Leer número de serie del dispositivo (SN, código 1103) para trazabilidad y anti-suplantación.
        if (TryReadDeviceSerial(out string deviceSerial))
        {
            string formattedSerial = FormatDeviceId(deviceSerial);
            _deviceSerial = formattedSerial;
            logger.LogInformation("Lector ZK9500 abierto. Serial: {DeviceSerial}", formattedSerial);
        }
        else
        {
            _deviceSerial = null;
            logger.LogInformation("Lector abierto en el índice 0. Serial no disponible.");
        }

        return true;
    }

    private FingerprintDeviceStatus GetLiveDeviceStatus()
    {
        logger.LogInformation("Verificando estado real del lector...");

        if (!sdk.IsAvailable)
            return CreateDeviceStatus(false, 0, sdk.LoadError ?? "El SDK ZKTeco no está disponible.");

        if (!EnsureSdkInitialized())
            return CreateDeviceStatus(false, 0, "El SDK ZKTeco no está inicializado.");

        if (!TryGetDeviceCount(out int deviceCount))
            return CreateDeviceStatus(false, 0, DeviceDisconnectedMessage);

        if (deviceCount <= 0)
        {
            logger.LogWarning(DeviceDisconnectedMessage);
            CloseCurrentDeviceHandle();
            return CreateDeviceStatus(false, 0, DeviceDisconnectedMessage);
        }

        if (!EnsureDeviceOpen())
            return CreateDeviceStatus(false, deviceCount, DeviceDisconnectedMessage);

        if (_databaseHandle == IntPtr.Zero)
        {
            InitializeDatabase();
        }

        bool connected = _deviceHandle != IntPtr.Zero;
        if (connected)
        {
            logger.LogInformation("Lector biométrico conectado.");
            return CreateDeviceStatus(true, deviceCount, "Lector biométrico conectado.");
        }

        CloseCurrentDeviceHandle();
        logger.LogWarning(DeviceDisconnectedMessage);
        return CreateDeviceStatus(false, deviceCount, DeviceDisconnectedMessage);
    }

    private bool EnsureSdkInitialized()
    {
        if (_initialized)
            return true;

        int retryInit = sdk.Init();
        if (retryInit != 0)
        {
            logger.LogDebug("Reinicio del SDK ZKTeco falló: {Message}", ZKTecoErrorMapper.ToMessage(retryInit));
            return false;
        }

        _initialized = true;
        logger.LogInformation("SDK ZKTeco inicializado bajo demanda.");
        return true;
    }

    private bool TryGetDeviceCount(out int deviceCount)
    {
        try
        {
            deviceCount = sdk.GetDeviceCount();
            return true;
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "No se pudo consultar la cantidad de lectores biométricos.");
            CloseCurrentDeviceHandle();
            deviceCount = 0;
            return false;
        }
    }

    private bool EnsureDeviceOpen()
    {
        if (_deviceHandle != IntPtr.Zero && IsBiometricOperationInProgress())
            return true;

        if (_deviceHandle != IntPtr.Zero)
            CloseCurrentDeviceHandle();

        if (!TryOpenDevice())
            return false;

        ConfigureCaptureBuffers();
        return true;
    }

    private bool IsBiometricOperationInProgress()
    {
        lock (_operationStateLock)
        {
            return _operationState is BiometricOperationState.Capturing
                or BiometricOperationState.Identifying
                or BiometricOperationState.Enrolling;
        }
    }

    private FingerprintDeviceStatus CreateDeviceStatus(bool connected, int deviceCount, string message)
    {
        return new FingerprintDeviceStatus(
            "device.status.result",
            connected,
            sdk.IsAvailable,
            _initialized,
            _deviceHandle != IntPtr.Zero,
            connected,
            deviceCount,
            message,
            DateTimeOffset.UtcNow);
    }

    private bool IsDeviceHandleValid()
    {
        if (_deviceHandle == IntPtr.Zero)
        {
            return false;
        }

        try
        {
            return sdk.TryGetParameter(_deviceHandle, 1, out int width) && width > 0
                || sdk.TryGetParameter(_deviceHandle, 2, out int height) && height > 0
                || TryReadDeviceSerial(out _);
        }
        catch (Exception ex)
        {
            logger.LogDebug(ex, "No se pudo validar el handle actual del lector.");
            return false;
        }
    }

    private bool IsDeviceDisconnected()
    {
        try
        {
            return sdk.GetDeviceCount() <= 0 || _deviceHandle == IntPtr.Zero || !IsDeviceHandleValid();
        }
        catch (Exception ex)
        {
            logger.LogDebug(ex, "La verificación viva del lector falló durante captura.");
            return true;
        }
    }

    private void CloseCurrentDeviceHandle()
    {
        if (_deviceHandle == IntPtr.Zero)
        {
            return;
        }

        try
        {
            logger.LogInformation("Handle anterior inválido. Cerrando lector...");
            sdk.CloseDevice(_deviceHandle);
        }
        catch (Exception ex)
        {
            logger.LogDebug(ex, "Error no crítico cerrando handle anterior del lector.");
        }
        finally
        {
            _deviceHandle = IntPtr.Zero;
            _deviceSerial = null;
        }
    }

    private void InitializeDatabase()
    {
        _databaseHandle = sdk.DBInit();
        if (_databaseHandle == IntPtr.Zero)
        {
            logger.LogWarning("No se pudo inicializar la base biométrica ZKTeco.");
            return;
        }

        logger.LogInformation("Base biométrica ZKTeco inicializada.");
    }

    private bool TryReadDeviceSerial(out string serial)
    {
        if (sdk.TryGetParameterString(_deviceHandle, 1103, out serial))
        {
            return true;
        }

        if (sdk.TryGetParameter(_deviceHandle, 1103, out int numericSerial) && numericSerial > 0)
        {
            serial = numericSerial.ToString(CultureInfo.InvariantCulture);
            return true;
        }

        serial = string.Empty;
        return false;
    }

    private static string FormatDeviceId(string serial)
    {
        string cleanSerial = serial.Trim();
        return cleanSerial.StartsWith("ZK", StringComparison.OrdinalIgnoreCase)
            ? cleanSerial
            : $"ZK9500-{cleanSerial}";
    }

    private void ConfigureCaptureBuffers()
    {
        if (sdk.TryGetParameter(_deviceHandle, 1, out int width))
        {
            _imageWidth = width;
            logger.LogInformation("Ancho de imagen ZKTeco detectado: {Width}.", _imageWidth);
        }

        if (sdk.TryGetParameter(_deviceHandle, 2, out int height))
        {
            _imageHeight = height;
            logger.LogInformation("Alto de imagen ZKTeco detectado: {ImageHeight}.", _imageHeight);
        }

        if (sdk.TryGetParameter(_deviceHandle, 106, out int imageDataSize))
        {
            _imageDataSize = imageDataSize;
            logger.LogInformation("Tamaño de datos de imagen ZKTeco detectado: {ImageDataSize} bytes.", _imageDataSize);
        }
        else
        {
            _imageDataSize = _imageWidth * _imageHeight;
            logger.LogInformation("Tamaño de datos de imagen ZKTeco no disponible. Usando tamaño alternativo: {ImageDataSize} bytes.", _imageDataSize);
        }

        int imageBufferSize = _imageDataSize > 0 ? _imageDataSize : _imageWidth * _imageHeight;
        _imageBuffer = new byte[imageBufferSize];
        _templateBuffer = new byte[_templateBufferSize];
        int imageBufferLength = _imageBuffer.Length;
        int templateBufferLength = _templateBuffer.Length;
        logger.LogInformation("Buffers de captura ZKTeco asignados. Buffer de imagen: {ImageBufferSize} bytes. Buffer de plantilla: {TemplateBufferSize} bytes.", imageBufferLength, templateBufferLength);
    }

    private static bool IsFatalCaptureError(int captureResult)
    {
        return captureResult is -2 or -3 or -4;
    }

    private static string GetPlaceFingerMessage(int step)
    {
        return step switch
        {
            1 => "Coloque el dedo por primera vez.",
            2 => "Coloque el dedo por segunda vez.",
            3 => "Coloque el dedo por tercera vez.",
            _ => $"Coloque el dedo. Muestra {step}."
        };
    }

    private static int GetPositiveConfigurationValue(IConfiguration configuration, string key, int fallback)
    {
        string? configuredValue = configuration[key];
        return int.TryParse(configuredValue, out int value) && value > 0 ? value : fallback;
    }

    internal static int GetStandardizedConfigurationValue(
        IConfiguration configuration,
        string key,
        int fallback,
        int maximum)
    {
        int value = GetPositiveConfigurationValue(configuration, key, fallback);
        return Math.Min(value, maximum);
    }

    internal static double GetStandardizedDoubleConfigurationValue(
        IConfiguration configuration,
        string key,
        double fallback,
        double maximum)
    {
        double value = GetPositiveDoubleConfigurationValue(configuration, key, fallback);
        return Math.Min(value, maximum);
    }

    private static double GetPositiveDoubleConfigurationValue(IConfiguration configuration, string key, double fallback)
    {
        string? configuredValue = configuration[key];
        return double.TryParse(configuredValue, NumberStyles.Any, CultureInfo.InvariantCulture, out double value) && value > 0
            ? value
            : fallback;
    }

    private static int ClampEnrollmentSamples(int configured, ILogger logger)
    {
        if (configured != DefaultEnrollmentSamples)
            logger.LogWarning(
                "Enrollment:RequiredSamples={Configured} se ignoró. ZKTeco DBMerge requiere exactamente {Required} muestras.",
                configured, DefaultEnrollmentSamples);
        return DefaultEnrollmentSamples;
    }
}
