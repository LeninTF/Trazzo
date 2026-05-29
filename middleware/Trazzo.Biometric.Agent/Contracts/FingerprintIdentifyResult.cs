namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintIdentifyResult(
    string Type,
    bool Success,
    string Message,
    string? TemplateBase64,
    EncryptedPayload? EncryptedTemplate,
    int TemplateSize,
    string? DeviceId,
    FingerprintQualityResult? Quality,
    DateTimeOffset CapturedAtUtc)
{
    public static FingerprintIdentifyResult Succeeded(
        byte[] template,
        int templateSize,
        FingerprintQualityResult? quality,
        string? deviceId = null,
        EncryptedPayload? encryptedTemplate = null)
    {
        string? plainBase64 = encryptedTemplate is null
            ? Convert.ToBase64String(template.AsSpan(0, templateSize))
            : null;

        return new FingerprintIdentifyResult(
            "fingerprint.identify.result",
            true,
            "Huella capturada correctamente para identificación.",
            plainBase64,
            encryptedTemplate,
            templateSize,
            deviceId,
            quality,
            DateTimeOffset.UtcNow);
    }

    public static FingerprintIdentifyResult Failed(string message, FingerprintQualityResult? quality = null)
    {
        return new FingerprintIdentifyResult(
            "fingerprint.identify.result",
            false,
            message,
            null,
            null,
            0,
            null,
            quality,
            DateTimeOffset.UtcNow);
    }
}
