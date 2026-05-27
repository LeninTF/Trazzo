using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Services;
using Trazzo.Biometric.Agent.ZKTeco;

namespace Trazzo.Biometric.Agent.Tests;

internal sealed class FakeZKTecoNativeSdk : IZKTecoNativeSdk
{
    public bool IsAvailable { get; init; } = true;

    public string? LoadError { get; init; }

    public int InitResult { get; init; }

    public int DeviceCount { get; init; }

    public IntPtr DeviceHandle { get; init; }

    public IntPtr DatabaseHandle { get; init; }

    public int CaptureResult { get; init; }

    public byte[] CapturedTemplate { get; init; } = [];

    public int InitCalls { get; private set; }

    public int OpenDeviceCalls { get; private set; }

    public int DBInitCalls { get; private set; }

    public int AcquireFingerprintCalls { get; private set; }

    public int Init()
    {
        InitCalls++;
        return InitResult;
    }

    public int Terminate() => 0;

    public int GetDeviceCount() => DeviceCount;

    public IntPtr OpenDevice(int index)
    {
        OpenDeviceCalls++;
        return DeviceHandle;
    }

    public int CloseDevice(IntPtr deviceHandle) => 0;

    public int AcquireFingerprint(IntPtr deviceHandle, byte[] imageBuffer, byte[] template, ref int size)
    {
        AcquireFingerprintCalls++;
        FillHighQualityFingerprintImage(imageBuffer);
        CapturedTemplate.CopyTo(template, 0);
        size = CapturedTemplate.Length;
        return CaptureResult;
    }

    public bool TryGetParameter(IntPtr deviceHandle, int parameterCode, out int value)
    {
        value = parameterCode switch
        {
            1 => 256,
            2 => 288,
            106 => 256 * 288,
            _ => 0
        };

        return value > 0;
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
        registeredTemplateSize = copyLength;
        return 0;
    }

    private static void FillHighQualityFingerprintImage(byte[] imageBuffer)
    {
        Array.Fill(imageBuffer, (byte)220);

        const int width = 256;
        int startX = 78;
        int endX = 178;
        int startY = 74;
        int endY = 214;

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

internal sealed class FakeBiometricScannerService : IBiometricScannerService
{
    public FingerprintDeviceStatus Status { get; init; } = new(
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

    public Task InitializeAsync(CancellationToken cancellationToken) => Task.CompletedTask;

    public Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(Status);
    }

    public Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(CaptureResult);
    }

    public Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(FingerprintIdentifyResult.Failed("No se pudo capturar la huella para identificación."));
    }

    public Task<FingerprintEnrollResult> EnrollFingerprintAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken cancellationToken)
    {
        return Task.FromResult(FingerprintEnrollResult.Failed("No se pudo enrolar la huella. Intente nuevamente."));
    }

    public FingerprintEnrollResult CancelEnrollment()
    {
        return FingerprintEnrollResult.Failed("Enrolamiento cancelado.");
    }

    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}

internal sealed class FakeAgentHealthService : IAgentHealthService
{
    public object GetHealthResult()
    {
        return new
        {
            type = "health.check.result",
            success = true,
            message = "El agente biométrico de Trazzo está en ejecución."
        };
    }
}
