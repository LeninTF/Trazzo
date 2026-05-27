namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintEnrollResult(
    string Type,
    bool Success,
    string Message,
    string? RegisteredTemplateBase64,
    int RegisteredTemplateSize,
    int CapturedSamples,
    DateTimeOffset CapturedAtUtc)
{
    public static FingerprintEnrollResult Succeeded(byte[] registeredTemplate, int registeredTemplateSize, int capturedSamples)
    {
        return new FingerprintEnrollResult(
            "fingerprint.enroll.result",
            true,
            "Huella enrolada correctamente.",
            Convert.ToBase64String(registeredTemplate.AsSpan(0, registeredTemplateSize)),
            registeredTemplateSize,
            capturedSamples,
            DateTimeOffset.UtcNow);
    }

    public static FingerprintEnrollResult Failed(string message, int capturedSamples = 0)
    {
        return new FingerprintEnrollResult(
            "fingerprint.enroll.result",
            false,
            message,
            null,
            0,
            capturedSamples,
            DateTimeOffset.UtcNow);
    }
}
