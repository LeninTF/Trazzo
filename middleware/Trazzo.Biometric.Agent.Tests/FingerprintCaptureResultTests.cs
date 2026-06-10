
using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintCaptureResultTests
{
    [Fact]
    public void Succeeded_EncodesOnlyTemplateBytesWithinTemplateSize()
    {
        byte[] template = [1, 2, 3, 4, 5];

        FingerprintCaptureResult result = FingerprintCaptureResult.Succeeded(template, 3);

        Assert.Equal("fingerprint.capture.result", result.Type);
        Assert.True(result.Success);
        Assert.Equal("Huella capturada correctamente.", result.Message);
        Assert.Equal(Convert.ToBase64String([1, 2, 3]), result.TemplateBase64);
        Assert.Equal(3, result.TemplateSize);
        Assert.Null(result.EncryptedTemplate);
        Assert.Null(result.DeviceId);
    }

    [Fact]
    public void Succeeded_WithEncryptedTemplate_NullsOutPlainBase64()
    {
        byte[] template = [1, 2, 3];
        EncryptedPayload encrypted = new("cipher", "key", "iv", "tag");

        FingerprintCaptureResult result = FingerprintCaptureResult.Succeeded(
            template,
            3,
            new FingerprintCaptureOptions(EncryptedTemplate: encrypted));

        Assert.True(result.Success);
        Assert.Null(result.TemplateBase64);
        Assert.Equal(encrypted, result.EncryptedTemplate);
        Assert.Equal(3, result.TemplateSize);
    }

    [Fact]
    public void Succeeded_WithDeviceId_IncludesSerialInResult()
    {
        byte[] template = [1, 2, 3];

        FingerprintCaptureResult result = FingerprintCaptureResult.Succeeded(
            template,
            3,
            new FingerprintCaptureOptions(DeviceId: "ZK9500-12345"));

        Assert.True(result.Success);
        Assert.Equal("ZK9500-12345", result.DeviceId);
    }

    [Fact]
    public void Failed_ReturnsFailurePayloadWithoutTemplate()
    {
        FingerprintCaptureResult result = FingerprintCaptureResult.Failed("No se encontró ningún lector biométrico.");

        Assert.Equal("fingerprint.capture.result", result.Type);
        Assert.False(result.Success);
        Assert.Equal("No se encontró ningún lector biométrico.", result.Message);
        Assert.Null(result.TemplateBase64);
        Assert.Null(result.EncryptedTemplate);
        Assert.Null(result.DeviceId);
        Assert.Equal(0, result.TemplateSize);
    }
}
