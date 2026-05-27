namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintIdentifyResult(
    string Type,
    bool Success,
    string Message,
    string? TemplateBase64,
    int TemplateSize,
    FingerprintQualityResult? Quality,
    DateTimeOffset CapturedAtUtc)
{
    public static FingerprintIdentifyResult Succeeded(byte[] template, int templateSize, FingerprintQualityResult? quality)
    {
        return new FingerprintIdentifyResult(
            "fingerprint.identify.result",
            true,
            "Huella capturada correctamente para identificación.",
            Convert.ToBase64String(template.AsSpan(0, templateSize)),
            templateSize,
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
            0,
            quality,
            DateTimeOffset.UtcNow);
    }
}
