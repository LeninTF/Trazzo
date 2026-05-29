namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintCaptureResult(
    string Type,
    bool Success,
    string Message,
    string? TemplateBase64,
    EncryptedPayload? EncryptedTemplate,
    int TemplateSize,
    string? DeviceId,
    string? FingerprintImageBase64,
    string? FingerprintImageMimeType,
    string? FingerprintImageDataUrl,
    FingerprintQualityResult? Quality,
    DateTimeOffset CapturedAtUtc)
{
    public static FingerprintCaptureResult Succeeded(
        byte[] template,
        int templateSize,
        string? deviceId = null,
        EncryptedPayload? encryptedTemplate = null,
        FingerprintQualityResult? quality = null,
        string? fingerprintImageBase64 = null,
        string? fingerprintImageMimeType = null,
        string? fingerprintImageDataUrl = null)
    {
        // TemplateBase64 solo se incluye cuando el cifrado no está configurado (modo desarrollo)
        string? plainBase64 = encryptedTemplate is null
            ? Convert.ToBase64String(template.AsSpan(0, templateSize))
            : null;

        return new FingerprintCaptureResult(
            "fingerprint.capture.result",
            true,
            "Huella capturada correctamente.",
            plainBase64,
            encryptedTemplate,
            templateSize,
            deviceId,
            fingerprintImageBase64,
            fingerprintImageMimeType,
            fingerprintImageDataUrl,
            quality,
            DateTimeOffset.UtcNow);
    }

    public static FingerprintCaptureResult Failed(
        string message,
        FingerprintQualityResult? quality = null,
        string? fingerprintImageBase64 = null,
        string? fingerprintImageMimeType = null,
        string? fingerprintImageDataUrl = null)
    {
        return new FingerprintCaptureResult(
            "fingerprint.capture.result",
            false,
            message,
            null,
            null,
            0,
            null,
            fingerprintImageBase64,
            fingerprintImageMimeType,
            fingerprintImageDataUrl,
            quality,
            DateTimeOffset.UtcNow);
    }
}
