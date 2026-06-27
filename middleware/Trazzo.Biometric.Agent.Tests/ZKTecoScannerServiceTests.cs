using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Configuration;
using Trazzo.Biometric.Agent.ZKTeco;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class ZKTecoScannerServiceTests
{
    [Fact]
    public async Task InitializeAsync_CuandoYaEstaInicializado_NoInicializaDosVeces()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        await using ZKTecoScannerService service = CreateService(sdk);

        await service.InitializeAsync(CancellationToken.None);
        await service.InitializeAsync(CancellationToken.None);

        Assert.Equal(1, sdk.InitCalls);
    }

    [Fact]
    public async Task InitializeAsync_CuandoSdkNoDisponible_NoInicializa()
    {
        FakeZKTecoNativeSdk sdk = new() { IsAvailable = false, LoadError = "missing dll" };
        await using ZKTecoScannerService service = CreateService(sdk);

        await service.InitializeAsync(CancellationToken.None);

        Assert.Equal(0, sdk.InitCalls);
    }

    [Fact]
    public async Task InitializeAsync_CuandoInitFalla_NoAbreDispositivo()
    {
        FakeZKTecoNativeSdk sdk = new() { InitResult = -3, DeviceCount = 1 };
        await using ZKTecoScannerService service = CreateService(sdk);

        await service.InitializeAsync(CancellationToken.None);

        Assert.Equal(1, sdk.InitCalls);
        Assert.Equal(0, sdk.OpenDeviceCalls);
    }

    [Fact]
    public async Task InitializeAsync_CuandoSdkLanzaExcepcion_NoPropaga()
    {
        FakeZKTecoNativeSdk sdk = new() { InitException = new InvalidOperationException("sdk failure") };
        await using ZKTecoScannerService service = CreateService(sdk);

        await service.InitializeAsync(CancellationToken.None);

        Assert.Equal(1, sdk.InitCalls);
    }

    [Fact]
    public async Task GetStatusAsync_CalledTwice_ReleasesSdkLock()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var first = await service.GetStatusAsync(CancellationToken.None);
        var second = await service.GetStatusAsync(CancellationToken.None);

        Assert.True(first.IsConnected);
        Assert.True(second.IsConnected);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_WhenDeviceIsMissing_ReturnsFailure()
    {
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 0
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("No se encontró ningún lector biométrico conectado.", result.Message);
        Assert.Null(result.TemplateBase64);
        Assert.Null(result.EncryptedTemplate);
        Assert.Equal(0, result.TemplateSize);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_WhenCaptureSucceeds_ReturnsTemplateBase64()
    {
        byte[] capturedTemplate = Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray();
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = capturedTemplate,
            CaptureResult = 0
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.True(result.Success);
        Assert.Equal("Huella capturada correctamente.", result.Message);
        // Sin crypto configurado, el template viaja en Base64 (modo desarrollo)
        Assert.Equal(Convert.ToBase64String(capturedTemplate), result.TemplateBase64);
        Assert.Null(result.EncryptedTemplate);
        Assert.Equal(capturedTemplate.Length, result.TemplateSize);
        Assert.Equal(1, sdk.InitCalls);
        Assert.Equal(2, sdk.OpenDeviceCalls);
        Assert.Equal(1, sdk.DBInitCalls);
        Assert.True(sdk.AcquireFingerprintCalls >= 1);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_WhenCaptureSucceeds_IncludesDeviceSerial()
    {
        byte[] capturedTemplate = Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray();
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = capturedTemplate,
            CaptureResult = 0,
            DeviceSerial = 12345
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.True(result.Success);
        Assert.Equal("ZK9500-12345", result.DeviceId);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_WhenCaptureSucceeds_ImageDimensionsAre300x400()
    {
        byte[] capturedTemplate = Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray();
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = capturedTemplate,
            CaptureResult = 0
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        // El buffer de imagen debe ser 300×400 = 120,000 bytes (FAP20 ZK9500)
        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.True(result.Success);
        // La captura exitosa implica que el buffer se dimensionó correctamente para 300×400
        Assert.True(sdk.AcquireFingerprintCalls >= 1);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoCapturaExitosa_RetornaTemplateBase64()
    {
        byte[] capturedTemplate = Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray();
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = capturedTemplate,
            CaptureResult = 0
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.IdentifyFingerprintAsync(CancellationToken.None);

        Assert.True(result.Success);
        Assert.Equal("Huella capturada correctamente para identificación.", result.Message);
        Assert.Equal(Convert.ToBase64String(capturedTemplate), result.TemplateBase64);
        Assert.Null(result.EncryptedTemplate);
        Assert.Equal(capturedTemplate.Length, result.TemplateSize);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoSinLector_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = new() { DeviceCount = 0 };
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.IdentifyFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("No se encontró ningún lector biométrico conectado.", result.Message);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandaTresCapturasExitosas_RetornaTemplateRegistrado()
    {
        byte[] capturedTemplate = Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray();
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = capturedTemplate,
            CaptureResult = 0
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var progressMessages = new List<string>();
        var result = await service.EnrollFingerprintAsync(
            (progress, _) => { progressMessages.Add(progress.Message); return Task.CompletedTask; },
            CancellationToken.None);

        Assert.True(result.Success);
        Assert.Equal("Huella enrolada correctamente.", result.Message);
        Assert.Equal(3, result.CapturedSamples);
        Assert.NotNull(result.RegisteredTemplateBase64);
        Assert.Equal(3, progressMessages.Count);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoYaHayOperacionEnProgreso_RetornaOcupado()
    {
        byte[] capturedTemplate = Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray();
        FakeZKTecoNativeSdk sdk = new()
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = capturedTemplate,
            CaptureResult = 0
        };

        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        // Lanzar primera captura sin awaitar
        Task<Trazzo.Biometric.Agent.Contracts.FingerprintCaptureResult> first =
            service.CaptureFingerprintAsync(CancellationToken.None);

        // Intentar segunda captura inmediatamente
        var second = await service.CaptureFingerprintAsync(CancellationToken.None);

        await first;

        Assert.False(second.Success);
        Assert.Contains("operación biométrica en progreso", second.Message);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoHayOperacionEnProgreso_RetornaOcupado()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureDelayMilliseconds: 200);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);
        Task<Trazzo.Biometric.Agent.Contracts.FingerprintCaptureResult> capture =
            service.CaptureFingerprintAsync(CancellationToken.None);
        await Task.Delay(50);

        var result = await service.IdentifyFingerprintAsync(CancellationToken.None);
        await capture;

        Assert.False(result.Success);
        Assert.Contains("operación biométrica en progreso", result.Message);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandoHayOperacionEnProgreso_RetornaOcupado()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureDelayMilliseconds: 200);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);
        Task<Trazzo.Biometric.Agent.Contracts.FingerprintCaptureResult> capture =
            service.CaptureFingerprintAsync(CancellationToken.None);
        await Task.Delay(50);

        var result = await service.EnrollFingerprintAsync(
            (_, _) => Task.CompletedTask,
            CancellationToken.None);
        await capture;

        Assert.False(result.Success);
        Assert.Contains("operación biométrica en progreso", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoSdkDevuelveErrorFatal_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureResult: -2, template: []);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Parámetro inválido del SDK ZKTeco.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoTemplateExcedeBuffer_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(
            template: [1],
            reportedTemplateSize: 4096);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Error interno: tamaño de plantilla excede el buffer.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoTemplateEsPequeno_RetornaCalidadRechazada()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(template: new byte[10]);
        await using ZKTecoScannerService service = CreateService(
            sdk,
            new Dictionary<string, string?> { ["Biometric:Quality:MinimumTemplateSize"] = "100" });
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Huella incompleta o mal posicionada. Coloque el dedo completo y centrado sobre el lector.", result.Message);
        Assert.NotNull(result.Quality);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoImagenNoTieneCalidad_RetornaRechazada()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(fillQualityImage: false);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.NotNull(result.Quality);
        Assert.False(result.Quality.IsAcceptable);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_WithImageEnabled_IncludesImageOnSuccess()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        await using ZKTecoScannerService service = CreateService(
            sdk,
            new Dictionary<string, string?>
            {
                ["Biometric:IncludeFingerprintImageInResponses"] = "true"
            });
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.True(result.Success);
        Assert.NotNull(result.FingerprintImageBase64);
        Assert.Equal("image/png", result.FingerprintImageMimeType);
        Assert.StartsWith("data:image/png;base64,", result.FingerprintImageDataUrl);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_WithImageEnabled_IncludesImageOnQualityFailure()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(fillQualityImage: false);
        await using ZKTecoScannerService service = CreateService(
            sdk,
            new Dictionary<string, string?>
            {
                ["Biometric:IncludeFingerprintImageInResponses"] = "true"
            });
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.NotNull(result.FingerprintImageBase64);
        Assert.Equal("image/png", result.FingerprintImageMimeType);
        Assert.StartsWith("data:image/png;base64,", result.FingerprintImageDataUrl);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoSdkLanzaExcepcion_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(
            captureException: new InvalidOperationException("capture failure"));
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Contains("capture failure", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoSdkCancela_RetornaSesionFinalizada()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(
            captureException: new OperationCanceledException());
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Lectura ignorada porque la sesión de captura ya finalizó.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoTokenYaEstaCancelado_NoIniciaSesion()
    {
        using CancellationTokenSource cancellation = new();
        cancellation.Cancel();
        await using ZKTecoScannerService service = CreateService(CreateConnectedSdk());

        var result = await service.CaptureFingerprintAsync(cancellation.Token);

        Assert.False(result.Success);
        Assert.Equal("Lectura ignorada porque la sesión de captura ya finalizó.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoNoDetectaHuella_RetornaTimeout()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureResult: -1, template: []);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Tiempo de espera agotado. Coloque el dedo en el lector.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoLecturaConsumeTimeout_NoEsperaPollingAdicional()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(
            captureResult: -1,
            template: [],
            captureDelayMilliseconds: 1100);
        await using ZKTecoScannerService service = CreateService(
            sdk,
            new Dictionary<string, string?>
            {
                ["Biometric:CapturePollingIntervalMilliseconds"] = "2000"
            });
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Tiempo de espera agotado. Coloque el dedo en el lector.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoLectorSeDesconectaDuranteCaptura_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        sdk.DeviceCountSequence = new Queue<int>([1, 1, 0]);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("El lector biométrico fue desconectado durante la captura.", result.Message);
    }

    [Fact]
    public async Task CaptureFingerprintAsync_CuandoDedoYaEstaApoyado_SolicitaRetirarlo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        await using ZKTecoScannerService service = CreateService(
            sdk,
            new Dictionary<string, string?>
            {
                ["Biometric:RequireFingerLiftBeforeNextCapture"] = "true"
            });
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.CaptureFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Retire el dedo del lector y vuelva a colocarlo para iniciar una nueva operación.", result.Message);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoSdkDevuelveErrorFatal_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureResult: -4, template: []);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.IdentifyFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("No se pudo capturar la huella para identificación.", result.Message);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoSdkLanzaExcepcion_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(
            captureException: new InvalidOperationException("identify failure"));
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.IdentifyFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("No se pudo capturar la huella para identificación.", result.Message);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoSdkCancela_RetornaSesionFinalizada()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(
            captureException: new OperationCanceledException());
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.IdentifyFingerprintAsync(CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("Lectura ignorada porque la sesión de captura ya finalizó.", result.Message);
    }

    [Fact]
    public async Task IdentifyFingerprintAsync_CuandoTokenYaEstaCancelado_NoIniciaSesion()
    {
        using CancellationTokenSource cancellation = new();
        cancellation.Cancel();
        await using ZKTecoScannerService service = CreateService(CreateConnectedSdk());

        var result = await service.IdentifyFingerprintAsync(cancellation.Token);

        Assert.False(result.Success);
        Assert.Equal("Lectura ignorada porque la sesión de captura ya finalizó.", result.Message);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandoNoHayLector_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = new() { DeviceCount = 0 };
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (_, _) => Task.CompletedTask,
            CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("No se encontró ningún lector biométrico conectado.", result.Message);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandoDBMergeFalla_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(dbMergeResult: -1);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (_, _) => Task.CompletedTask,
            CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal(3, result.CapturedSamples);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandoDBMergeDevuelveTamanoCero_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        sdk.DBMergeTemplateSize = 0;
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (_, _) => Task.CompletedTask,
            CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal(3, result.CapturedSamples);
    }
    [Fact]
    public async Task EnrollFingerprintAsync_CuandoCallbackLanza_RetornaFallo()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (_, _) => Task.FromException(new InvalidOperationException("callback failure")),
            CancellationToken.None);

        Assert.False(result.Success);
        Assert.Equal("No se pudo enrolar la huella. Intente nuevamente.", result.Message);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandoCapturaEsRechazada_ReintentaMismaMuestra()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        sdk.FillQualityImageSequence = new Queue<bool>([false, true, true, true]);
        List<string> progress = [];
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (update, _) =>
            {
                progress.Add(update.Message);
                return Task.CompletedTask;
            },
            CancellationToken.None);

        Assert.True(result.Success);
        Assert.Contains(
            progress,
            message => message.Contains("Huella incompleta", StringComparison.Ordinal));
        Assert.True(sdk.AcquireFingerprintCalls >= 4);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_WhenFingerLiftRequired_WaitsBetweenSamples()
    {
        // Múltiples -1 por lift: IsFingerAlreadyOnReaderAsync puede consumir 0 o más
        // items por llamada (depende de si la ventana de 1 ms expira antes de iterar).
        // Los -1 extra aseguran que siempre devuelva false sin agotar la cola y exponer
        // el fallback CaptureResult=0, que causaría un loop infinito en CI.
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk();
        sdk.CaptureResultSequence = new Queue<int>([
            0, -1, -1, -1, -1, -1,   // muestra 1 + retiro
            0, -1, -1, -1, -1, -1,   // muestra 2 + retiro
            0                          // muestra 3
        ]);
        List<string> progress = [];
        await using ZKTecoScannerService service = CreateService(
            sdk,
            new Dictionary<string, string?>
            {
                ["Enrollment:RequireFingerLiftBetweenSamples"] = "true"
            });
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (update, _) =>
            {
                progress.Add(update.Message);
                return Task.CompletedTask;
            },
            cts.Token);

        Assert.True(result.Success);
        Assert.Equal(2, progress.Count(message => message == "Retire el dedo del lector."));
    }

    [Fact]
    public async Task EnrollFingerprintAsync_WhenCancellationRequested_ReturnsCancelled()
    {
        using CancellationTokenSource cancellation = new();
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureDelayMilliseconds: 100);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);

        var result = await service.EnrollFingerprintAsync(
            (update, _) =>
            {
                cancellation.Cancel();
                return Task.CompletedTask;
            },
            cancellation.Token);

        Assert.False(result.Success);
        Assert.Equal("Enrolamiento cancelado.", result.Message);
    }

    [Fact]
    public async Task EnrollFingerprintAsync_CuandoTokenYaEstaCancelado_NoIniciaSesion()
    {
        using CancellationTokenSource cancellation = new();
        cancellation.Cancel();
        await using ZKTecoScannerService service = CreateService(CreateConnectedSdk());

        var result = await service.EnrollFingerprintAsync(
            (_, _) => Task.CompletedTask,
            cancellation.Token);

        Assert.False(result.Success);
        Assert.Equal("Enrolamiento cancelado.", result.Message);
    }

    [Fact]
    public async Task CancelEnrollment_CuandoNoHayOperacion_RetornaFallo()
    {
        await using ZKTecoScannerService service = CreateService(new FakeZKTecoNativeSdk());

        var result = service.CancelEnrollment();

        Assert.Equal("No hay un enrolamiento activo.", result.Message);
    }

    [Fact]
    public async Task CancelEnrollment_CuandoEnrolamientoActivo_CancelaOperacion()
    {
        FakeZKTecoNativeSdk sdk = CreateConnectedSdk(captureDelayMilliseconds: 200);
        await using ZKTecoScannerService service = CreateService(sdk);
        await service.InitializeAsync(CancellationToken.None);
        Task<Trazzo.Biometric.Agent.Contracts.FingerprintEnrollResult> enrollment =
            service.EnrollFingerprintAsync((_, _) => Task.CompletedTask, CancellationToken.None);
        await Task.Delay(50);

        var cancellation = service.CancelEnrollment();
        var result = await enrollment;

        Assert.Equal("Enrolamiento cancelado.", cancellation.Message);
        Assert.False(result.Success);
    }

    private static ZKTecoScannerService CreateService(
        FakeZKTecoNativeSdk sdk,
        Dictionary<string, string?>? overrides = null)
    {
        Dictionary<string, string?> settings = new()
        {
            ["Biometric:CaptureTimeoutSeconds"] = "1",
            ["Biometric:CapturePollingIntervalMilliseconds"] = "1",
            ["Biometric:PostCaptureCooldownMilliseconds"] = "1",
            ["Biometric:TemplateBufferSize"] = "2048",
            ["Biometric:RequireFingerLiftBeforeNextCapture"] = "false",
            ["Biometric:Quality:MinimumTemplateSize"] = "1",
            ["Biometric:Quality:MinimumForegroundCoveragePercent"] = "10",
            ["Biometric:Quality:MaximumForegroundCoveragePercent"] = "80",
            ["Biometric:Quality:MinimumContrastScore"] = "20",
            ["Biometric:Quality:RequireCenteredFingerprint"] = "true",
            ["Biometric:Quality:CenterTolerancePercent"] = "35",
            ["Enrollment:RequireFingerLiftBetweenSamples"] = "false"
        };
        if (overrides is not null)
        {
            foreach ((string key, string? value) in overrides)
                settings[key] = value;
        }

        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(settings)
            .Build();

        return new ZKTecoScannerService(
            sdk,
            new FakeCryptographyService(),
            configuration,
            NullLogger<ZKTecoScannerService>.Instance);
    }

    private static FakeZKTecoNativeSdk CreateConnectedSdk(
        int captureResult = 0,
        byte[]? template = null,
        int? reportedTemplateSize = null,
        bool fillQualityImage = true,
        Exception? captureException = null,
        int captureDelayMilliseconds = 0,
        int dbMergeResult = 0)
    {
        return new FakeZKTecoNativeSdk
        {
            DeviceCount = 1,
            DeviceHandle = new IntPtr(123),
            DatabaseHandle = new IntPtr(456),
            CapturedTemplate = template ?? Enumerable.Range(0, 512).Select(value => (byte)(value % 255)).ToArray(),
            CaptureResult = captureResult,
            ReportedTemplateSize = reportedTemplateSize,
            FillQualityImage = fillQualityImage,
            CaptureException = captureException,
            CaptureDelayMilliseconds = captureDelayMilliseconds,
            DBMergeResult = dbMergeResult
        };
    }
}
