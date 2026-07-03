using System.Net;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Queue;
using Trazzo.Biometric.Agent.Services;
using Trazzo.Biometric.Agent.ZKTeco;

namespace Trazzo.Biometric.Agent.Tests;

internal sealed class FakeZKTecoNativeSdk : IZKTecoNativeSdk
{
    public bool IsAvailable { get; init; } = true;

    public string? LoadError { get; init; }

    public int InitResult { get; init; }

    public int DeviceCount { get; set; }

    public Queue<int>? DeviceCountSequence { get; set; }

    public IntPtr DeviceHandle { get; init; }

    public IntPtr DatabaseHandle { get; init; }

    public int CaptureResult { get; init; }

    public Queue<int>? CaptureResultSequence { get; set; }

    public byte[] CapturedTemplate { get; init; } = [];

    public Exception? InitException { get; init; }

    public Exception? CaptureException { get; init; }

    public int CaptureDelayMilliseconds { get; init; }

    public int? ReportedTemplateSize { get; init; }

    public bool FillQualityImage { get; init; } = true;

    public Queue<bool>? FillQualityImageSequence { get; set; }

    public int DBMergeResult { get; init; }

    public int? DBMergeTemplateSize { get; set; }

    // Serial por defecto que simula un ZK9500 real
    public int DeviceSerial { get; init; } = 12345;

    public bool SerialAvailable { get; init; } = true;

    public bool ImageDataSizeAvailable { get; init; } = true;

    public Exception? GetDeviceCountException { get; init; }

    public int InitCalls { get; private set; }

    public int OpenDeviceCalls { get; private set; }

    public int DBInitCalls { get; private set; }

    public int AcquireFingerprintCalls { get; private set; }

    public int Init()
    {
        InitCalls++;
        if (InitException is not null)
            throw InitException;

        return InitResult;
    }

    public int Terminate() => 0;

    public int GetDeviceCount()
    {
        if (GetDeviceCountException is not null)
            throw GetDeviceCountException;

        if (DeviceCountSequence is { Count: > 0 })
            return DeviceCountSequence.Dequeue();

        return DeviceCount;
    }

    public IntPtr OpenDevice(int index)
    {
        OpenDeviceCalls++;
        return DeviceHandle;
    }

    public int CloseDevice(IntPtr deviceHandle) => 0;

    public int AcquireFingerprint(IntPtr deviceHandle, byte[] imageBuffer, byte[] template, ref int size)
    {
        AcquireFingerprintCalls++;
        if (CaptureDelayMilliseconds > 0)
            Thread.Sleep(CaptureDelayMilliseconds);
        if (CaptureException is not null)
            throw CaptureException;

        bool fillQualityImage = FillQualityImageSequence is { Count: > 0 }
            ? FillQualityImageSequence.Dequeue()
            : FillQualityImage;
        if (fillQualityImage)
            FillHighQualityFingerprintImage(imageBuffer);
        else
            Array.Fill(imageBuffer, (byte)255);

        int captureResult = CaptureResultSequence is { Count: > 0 }
            ? CaptureResultSequence.Dequeue()
            : CaptureResult;
        int copyLength = Math.Min(CapturedTemplate.Length, template.Length);
        CapturedTemplate.AsSpan(0, copyLength).CopyTo(template);
        size = ReportedTemplateSize ?? (captureResult == 0 ? CapturedTemplate.Length : 0);
        return captureResult;
    }

    public bool TryGetParameter(IntPtr deviceHandle, int parameterCode, out int value)
    {
        // ZK9500: 300×400 px (FAP20) según hardware selection guide
        value = parameterCode switch
        {
            1 => 300,
            2 => 400,
            106 => ImageDataSizeAvailable ? 300 * 400 : 0,
            1103 => SerialAvailable ? DeviceSerial : 0,
            _ => 0
        };

        return value > 0;
    }

    public bool TryGetParameterString(IntPtr deviceHandle, int parameterCode, out string value)
    {
        if (!SerialAvailable && parameterCode == 1103)
        {
            value = string.Empty;
            return false;
        }

        value = parameterCode == 1103 ? DeviceSerial.ToString() : string.Empty;
        return value.Length > 0;
    }

    public IntPtr DBInit()
    {
        DBInitCalls++;
        return DatabaseHandle;
    }

    public int DBFree(IntPtr databaseHandle) => 0;

    public int DBMatch(IntPtr databaseHandle, byte[] template1, byte[] template2) => 0;

    public int DBIdentify(IntPtr databaseHandle, byte[] template, ref int fingerId, ref int score) => 0;

    public int DBMerge(IntPtr databaseHandle, byte[] template1, byte[] template2, byte[] template3, byte[] registeredTemplate, ref int registeredTemplateSize)
    {
        int copyLength = Math.Min(template1.Length, registeredTemplate.Length);
        Array.Copy(template1, registeredTemplate, copyLength);
        registeredTemplateSize = DBMergeTemplateSize ?? copyLength;
        return DBMergeResult;
    }

    private static void FillHighQualityFingerprintImage(byte[] imageBuffer)
    {
        Array.Fill(imageBuffer, (byte)220);

        // ZK9500: imagen de 300×400 px; área oscura centrada simulando una huella real
        const int width = 300;
        int startX = 100;
        int endX = 200;
        int startY = 130;
        int endY = 270;

        for (int y = startY; y < endY; y++)
        {
            for (int x = startX; x < endX; x++)
            {
                int index = y * width + x;
                if (index >= 0 && index < imageBuffer.Length)
                {
                    imageBuffer[index] = 45;
                }
            }
        }
    }
}

internal sealed class FakeCryptographyService : ICryptographyService
{
    public bool IsConfigured => false;

    public Task InitializeAsync(CancellationToken cancellationToken) => Task.CompletedTask;

    public EncryptedPayload? TryEncryptTemplate(byte[] template, int templateSize) => null;
}

internal sealed class FakeBiometricScannerService : IBiometricScannerService
{
    public FingerprintDeviceStatus Status { get; set; } = new(
        "device.status.result",
        Success: false,
        IsSdkAvailable: true,
        IsInitialized: true,
        IsDeviceOpen: false,
        IsConnected: false,
        DeviceCount: 0,
        Message: "No se encontró ningún lector biométrico.",
        CheckedAtUtc: DateTimeOffset.UtcNow);

    public FingerprintCaptureResult CaptureResult { get; init; } =
        FingerprintCaptureResult.Failed("No se encontró ningún lector biométrico.");

    public FingerprintIdentifyResult IdentifyResult { get; init; } =
        FingerprintIdentifyResult.Failed("No se pudo capturar la huella para identificación.");

    public FingerprintEnrollResult EnrollResult { get; init; } =
        FingerprintEnrollResult.Failed("No se pudo enrolar la huella. Intente nuevamente.");

    public Exception? StatusException { get; set; }
    public Exception? CaptureException { get; set; }
    public FingerprintEnrollProgress? EnrollmentProgress { get; init; }

    public Task InitializeAsync(CancellationToken cancellationToken) => Task.CompletedTask;

    public Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken)
    {
        if (StatusException is not null)
            return Task.FromException<FingerprintDeviceStatus>(StatusException);

        return Task.FromResult(Status);
    }

    public Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken)
    {
        if (CaptureException is not null)
            return Task.FromException<FingerprintCaptureResult>(CaptureException);

        return Task.FromResult(CaptureResult);
    }

    public Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(IdentifyResult);
    }

    public async Task<FingerprintEnrollResult> EnrollFingerprintAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken cancellationToken)
    {
        if (EnrollmentProgress is not null)
            await progressCallback(EnrollmentProgress, cancellationToken);

        return EnrollResult;
    }

    public FingerprintEnrollResult CancelEnrollment()
    {
        return FingerprintEnrollResult.Failed("Enrolamiento cancelado.");
    }

    public Task<FingerprintMatchResult> MatchFingerprintAgainstTemplatesAsync(
        IReadOnlyList<(int Index, byte[] Template)> templates,
        CancellationToken cancellationToken)
    {
        return Task.FromResult(FingerprintMatchResult.Failed("No se encontró ningún lector biométrico."));
    }

    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}

internal sealed class FakeEventQueue : IEventQueue
{
    public List<BiometricEvent> Enqueued { get; } = [];
    public List<long[]> SentIds { get; } = [];
    public List<long> FailedIds { get; } = [];
    public IReadOnlyList<BiometricEvent> PendingToReturn { get; init; } = [];
    public int PendingCount { get; init; }

    public Task EnqueueAsync(BiometricEvent biometricEvent, CancellationToken cancellationToken = default)
    {
        Enqueued.Add(biometricEvent);
        return Task.CompletedTask;
    }

    public Task<IReadOnlyList<BiometricEvent>> GetPendingAsync(int limit = 50, CancellationToken cancellationToken = default)
        => Task.FromResult(PendingToReturn);

    public Task MarkSentAsync(IEnumerable<long> ids, CancellationToken cancellationToken = default)
    {
        SentIds.Add([.. ids]);
        return Task.CompletedTask;
    }

    public Task MarkFailedAsync(long id, CancellationToken cancellationToken = default)
    {
        FailedIds.Add(id);
        return Task.CompletedTask;
    }

    public Task<int> GetPendingCountAsync(CancellationToken cancellationToken = default)
        => Task.FromResult(PendingCount);

    public Task PruneAsync(TimeSpan maxAge, CancellationToken cancellationToken = default)
        => Task.CompletedTask;
}

internal sealed class FakeAttendanceMarkingClient : IAttendanceMarkingClient
{
    public bool Result { get; init; }
    public int Calls { get; private set; }
    public EncryptedPayload? LastEncryptedTemplate { get; private set; }
    public string? LastDeviceId { get; private set; }
    public DateTimeOffset LastCapturedAtUtc { get; private set; }

    public Task<bool> TryMarkAsync(
        EncryptedPayload encryptedTemplate,
        string? deviceId,
        DateTimeOffset capturedAtUtc,
        CancellationToken cancellationToken)
    {
        Calls++;
        LastEncryptedTemplate = encryptedTemplate;
        LastDeviceId = deviceId;
        LastCapturedAtUtc = capturedAtUtc;
        return Task.FromResult(Result);
    }
}

internal sealed class FakeAgentHealthService : IAgentHealthService
{
    public Exception? Exception { get; init; }

    public object GetHealthResult()
    {
        if (Exception is not null)
            throw Exception;

        return new
        {
            type = "health.check.result",
            success = true,
            message = "El agente biométrico de Trazzo está en ejecución."
        };
    }
}

internal sealed class MockHttpMessageHandler : HttpMessageHandler
{
    public HttpRequestMessage? LastRequest { get; private set; }
    public string? LastRequestBody { get; private set; }
    public HttpStatusCode ResponseStatusCode { get; init; } = HttpStatusCode.OK;
    public string? ResponseContent { get; init; }

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request, CancellationToken cancellationToken)
    {
        LastRequest = request;
        LastRequestBody = request.Content is not null
            ? await request.Content.ReadAsStringAsync(cancellationToken)
            : null;
        return new HttpResponseMessage(ResponseStatusCode)
        {
            Content = ResponseContent is null ? null : new StringContent(ResponseContent)
        };
    }
}
