namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintCaptureOptions(
    string? DeviceId = null,
    EncryptedPayload? EncryptedTemplate = null,
    FingerprintQualityResult? Quality = null,
    string? FingerprintImageBase64 = null,
    string? FingerprintImageMimeType = null,
    string? FingerprintImageDataUrl = null);

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
        FingerprintCaptureOptions? options = null)
    {
        options ??= new FingerprintCaptureOptions();
        string? plainBase64 = options.EncryptedTemplate is null && BiometricSecurityGates.AllowPlaintextTemplateFallback
            ? Convert.ToBase64String(template.AsSpan(0, templateSize))
            : null;

        return new FingerprintCaptureResult(
            "fingerprint.capture.result",
            true,
            "Huella capturada correctamente.",
            plainBase64,
            options.EncryptedTemplate,
            templateSize,
            options.DeviceId,
            options.FingerprintImageBase64,
            options.FingerprintImageMimeType,
            options.FingerprintImageDataUrl,
            options.Quality,
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
