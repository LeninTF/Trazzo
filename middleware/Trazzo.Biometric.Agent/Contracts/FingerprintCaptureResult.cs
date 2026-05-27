namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintCaptureResult(
    string Type,
    bool Success,
    string Message,
    string? TemplateBase64,
    int TemplateSize,
    string? FingerprintImageBase64,
    string? FingerprintImageMimeType,
    string? FingerprintImageDataUrl,
    FingerprintQualityResult? Quality,
    DateTimeOffset CapturedAtUtc)
{
    public static FingerprintCaptureResult Succeeded(
        byte[] template,
        int templateSize,
        FingerprintQualityResult? quality = null,
        string? fingerprintImageBase64 = null,
        string? fingerprintImageMimeType = null,
        string? fingerprintImageDataUrl = null)
    {
        return new FingerprintCaptureResult(
            "fingerprint.capture.result",
            true,
            "Huella capturada correctamente.",
            Convert.ToBase64String(template.AsSpan(0, templateSize)),
            templateSize,
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
            0,
            fingerprintImageBase64,
            fingerprintImageMimeType,
            fingerprintImageDataUrl,
            quality,
            DateTimeOffset.UtcNow);
    }
}
