namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintEnrollResult(
    string Type,
    bool Success,
    string Message,
    string? RegisteredTemplateBase64,
    EncryptedPayload? EncryptedRegisteredTemplate,
    int RegisteredTemplateSize,
    string? DeviceId,
    int CapturedSamples,
    DateTimeOffset CapturedAtUtc)
{
    public static FingerprintEnrollResult Succeeded(
        byte[] registeredTemplate,
        int registeredTemplateSize,
        int capturedSamples,
        string? deviceId = null,
        EncryptedPayload? encryptedTemplate = null)
    {
        string? plainBase64 = encryptedTemplate is null && BiometricSecurityGates.AllowPlaintextTemplateFallback
            ? Convert.ToBase64String(registeredTemplate.AsSpan(0, registeredTemplateSize))
            : null;

        return new FingerprintEnrollResult(
            "fingerprint.enroll.result",
            true,
            "Huella enrolada correctamente.",
            plainBase64,
            encryptedTemplate,
            registeredTemplateSize,
            deviceId,
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
            null,
            0,
            null,
            capturedSamples,
            DateTimeOffset.UtcNow);
    }
}
