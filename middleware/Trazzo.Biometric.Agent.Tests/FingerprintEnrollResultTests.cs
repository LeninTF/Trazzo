using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintEnrollResultTests
{
    [Fact]
    public void Succeeded_WithoutEncryption_ReturnsPlainTemplate_WhenPlaintextGateOpen()
    {
        byte[] template = [1, 2, 3, 4];

        bool previousGate = BiometricSecurityGates.AllowPlaintextTemplateFallback;
        BiometricSecurityGates.AllowPlaintextTemplateFallback = true;
        try
        {
            FingerprintEnrollResult result = FingerprintEnrollResult.Succeeded(
                template,
                registeredTemplateSize: 3,
                capturedSamples: 3,
                deviceId: "ZK9500-123");

            Assert.Equal("fingerprint.enroll.result", result.Type);
            Assert.True(result.Success);
            Assert.Equal("Huella enrolada correctamente.", result.Message);
            Assert.Equal(Convert.ToBase64String([1, 2, 3]), result.RegisteredTemplateBase64);
            Assert.Null(result.EncryptedRegisteredTemplate);
            Assert.Equal(3, result.RegisteredTemplateSize);
            Assert.Equal("ZK9500-123", result.DeviceId);
            Assert.Equal(3, result.CapturedSamples);
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
            FingerprintEnrollResult result = FingerprintEnrollResult.Succeeded(
                [1, 2, 3, 4],
                registeredTemplateSize: 3,
                capturedSamples: 3);

            Assert.True(result.Success);
            Assert.Null(result.RegisteredTemplateBase64);
            Assert.Null(result.EncryptedRegisteredTemplate);
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

        FingerprintEnrollResult result = FingerprintEnrollResult.Succeeded(
            [1, 2, 3],
            registeredTemplateSize: 3,
            capturedSamples: 3,
            encryptedTemplate: encrypted);

        Assert.Null(result.RegisteredTemplateBase64);
        Assert.Equal(encrypted, result.EncryptedRegisteredTemplate);
    }

    [Fact]
    public void Failed_ReturnsFailureWithoutTemplate()
    {
        FingerprintEnrollResult result = FingerprintEnrollResult.Failed("Enrolamiento cancelado.", capturedSamples: 2);

        Assert.Equal("fingerprint.enroll.result", result.Type);
        Assert.False(result.Success);
        Assert.Equal("Enrolamiento cancelado.", result.Message);
        Assert.Null(result.RegisteredTemplateBase64);
        Assert.Null(result.EncryptedRegisteredTemplate);
        Assert.Equal(0, result.RegisteredTemplateSize);
        Assert.Null(result.DeviceId);
        Assert.Equal(2, result.CapturedSamples);
        Assert.NotEqual(default, result.CapturedAtUtc);
    }
}
