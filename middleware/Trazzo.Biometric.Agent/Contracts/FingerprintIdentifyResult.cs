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
    /// <summary>
    /// Estado de entrega de la marcación al backend: <c>"marked"</c> (marcación síncrona OK),
    /// <c>"queued"</c> (encolada para reenvío offline) o <c>"not_transmitted"</c> (no se envió;
    /// típicamente falta la clave RSA de cifrado). Permite al frontend distinguir "asistencia
    /// registrada" de "solo se capturó la huella".
    /// </summary>
    public string? DeliveryStatus { get; init; }

    public static FingerprintIdentifyResult Succeeded(
        byte[] template,
        int templateSize,
        FingerprintQualityResult? quality,
        string? deviceId = null,
        EncryptedPayload? encryptedTemplate = null)
    {
        string? plainBase64 = encryptedTemplate is null && BiometricSecurityGates.AllowPlaintextTemplateFallback
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
