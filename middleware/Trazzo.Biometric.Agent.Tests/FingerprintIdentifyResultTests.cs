using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintIdentifyResultTests
{
    [Fact]
    public void Succeeded_WithoutEncryption_ReturnsPlainTemplate_WhenPlaintextGateOpen()
    {
        FingerprintQualityResult quality = CreateQuality();

        bool previousGate = BiometricSecurityGates.AllowPlaintextTemplateFallback;
        BiometricSecurityGates.AllowPlaintextTemplateFallback = true;
        try
        {
            FingerprintIdentifyResult result = FingerprintIdentifyResult.Succeeded(
                [1, 2, 3, 4],
                templateSize: 3,
                quality,
                deviceId: "ZK9500-123");

            Assert.Equal("fingerprint.identify.result", result.Type);
            Assert.True(result.Success);
            Assert.Equal("Huella capturada correctamente para identificación.", result.Message);
            Assert.Equal(Convert.ToBase64String([1, 2, 3]), result.TemplateBase64);
            Assert.Null(result.EncryptedTemplate);
            Assert.Equal(3, result.TemplateSize);
            Assert.Equal("ZK9500-123", result.DeviceId);
            Assert.Equal(quality, result.Quality);
            Assert.NotEqual(default, result.CapturedAtUtc);
        }
        finally
        {
            BiometricSecurityGates.AllowPlaintextTemplateFallback = previousGate;
        }
    }

    [Fact]
    public void Succeeded_WithoutEncryption_OmitsPlainTemplate_WhenGateClosed()
    {
        bool previousGate = BiometricSecurityGates.AllowPlaintextTemplateFallback;
        BiometricSecurityGates.AllowPlaintextTemplateFallback = false;
        try
        {
            FingerprintIdentifyResult result = FingerprintIdentifyResult.Succeeded(
                [1, 2, 3, 4],
                templateSize: 3,
                quality: null);

            Assert.True(result.Success);
            Assert.Null(result.TemplateBase64);
            Assert.Null(result.EncryptedTemplate);
        }
        finally
        {
            BiometricSecurityGates.AllowPlaintextTemplateFallback = previousGate;
        }
    }

    [Fact]
    public void Succeeded_WithEncryption_OmitsPlainTemplate()
    {
        EncryptedPayload encrypted = new("cipher", "key", "iv", "tag");

        FingerprintIdentifyResult result = FingerprintIdentifyResult.Succeeded(
            [1, 2, 3],
            templateSize: 3,
            quality: null,
            encryptedTemplate: encrypted);

        Assert.Null(result.TemplateBase64);
        Assert.Equal(encrypted, result.EncryptedTemplate);
        Assert.Null(result.DeviceId);
        Assert.Null(result.Quality);
    }

    [Fact]
    public void Failed_ReturnsFailureWithQuality()
    {
        FingerprintQualityResult quality = CreateQuality();

        FingerprintIdentifyResult result = FingerprintIdentifyResult.Failed(
            "No se pudo identificar la huella.",
            quality);

        Assert.Equal("fingerprint.identify.result", result.Type);
        Assert.False(result.Success);
        Assert.Equal("No se pudo identificar la huella.", result.Message);
        Assert.Null(result.TemplateBase64);
        Assert.Null(result.EncryptedTemplate);
        Assert.Equal(0, result.TemplateSize);
        Assert.Null(result.DeviceId);
        Assert.Equal(quality, result.Quality);
        Assert.NotEqual(default, result.CapturedAtUtc);
    }

    private static FingerprintQualityResult CreateQuality()
    {
        return new FingerprintQualityResult(
            IsAcceptable: true,
            ForegroundPixelCount: 100,
            ForegroundCoveragePercent: 30,
            ContrastScore: 50,
            IsCentered: true,
            Message: "Calidad aceptable.");
    }
}
