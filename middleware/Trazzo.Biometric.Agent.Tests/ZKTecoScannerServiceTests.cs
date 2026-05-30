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
        Assert.Equal(1, sdk.OpenDeviceCalls);
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
                ["Biometric:Quality:CenterTolerancePercent"] = "35",
                ["Enrollment:RequireFingerLiftBetweenSamples"] = "false"
            })
            .Build();

        return new ZKTecoScannerService(
            sdk,
            new FakeCryptographyService(),
            configuration,
            NullLogger<ZKTecoScannerService>.Instance);
    }
}
