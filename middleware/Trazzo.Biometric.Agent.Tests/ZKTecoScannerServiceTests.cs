using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Configuration;
using Trazzo.Biometric.Agent.ZKTeco;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class ZKTecoScannerServiceTests
{
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
        Assert.Equal(Convert.ToBase64String(capturedTemplate), result.TemplateBase64);
        Assert.Equal(capturedTemplate.Length, result.TemplateSize);
        Assert.Equal(1, sdk.InitCalls);
        Assert.Equal(1, sdk.OpenDeviceCalls);
        Assert.Equal(1, sdk.DBInitCalls);
        Assert.True(sdk.AcquireFingerprintCalls >= 1);
    }

    private static ZKTecoScannerService CreateService(FakeZKTecoNativeSdk sdk)
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Biometric:CaptureTimeoutSeconds"] = "1",
                ["Biometric:CapturePollingIntervalMilliseconds"] = "1",
                ["Biometric:TemplateBufferSize"] = "2048",
                ["Biometric:RequireFingerLiftBeforeNextCapture"] = "false",
                ["Biometric:Quality:MinimumTemplateSize"] = "1",
                ["Biometric:Quality:MinimumForegroundCoveragePercent"] = "10",
                ["Biometric:Quality:MaximumForegroundCoveragePercent"] = "80",
                ["Biometric:Quality:MinimumContrastScore"] = "20",
                ["Biometric:Quality:RequireCenteredFingerprint"] = "true",
                ["Biometric:Quality:CenterTolerancePercent"] = "35"
            })
            .Build();

        return new ZKTecoScannerService(sdk, configuration, NullLogger<ZKTecoScannerService>.Instance);
    }
}
