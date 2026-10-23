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

    /// <summary>
    /// <c>true</c> si la captura fue identificada 1:N contra el padrón enrolado (ZKFinger
    /// DBIdentify). <c>false</c> cuando no coincide con ninguna huella enrolada (p. ej. la palma
    /// de la mano o un dedo no enrolado): en ese caso no se registra asistencia.
    /// </summary>
    public bool Matched { get; init; }

    /// <summary>Referencia del usuario cuyo dedo enrolado coincidió (null si no hubo match).</summary>
    public string? MatchedUserReference { get; init; }

    /// <summary>Score de similitud del match 1:N (0 si no hubo match).</summary>
    public int MatchScore { get; init; }

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

    /// <summary>Captura identificada 1:N contra el padrón enrolado.</summary>
    public static FingerprintIdentifyResult Identified(
        byte[] template,
        int templateSize,
        FingerprintQualityResult? quality,
        string? deviceId,
        EncryptedPayload? encryptedTemplate,
        string matchedUserReference,
        int matchScore)
    {
        FingerprintIdentifyResult baseResult = Succeeded(template, templateSize, quality, deviceId, encryptedTemplate);
        return baseResult with
        {
            Message = "Huella identificada correctamente.",
            Matched = true,
            MatchedUserReference = matchedUserReference,
            MatchScore = matchScore
        };
    }

    /// <summary>
    /// Captura válida pero que no coincide con ninguna huella enrolada. Success=false para que
    /// no se registre asistencia (así se rechaza la palma o un dedo no enrolado).
    /// </summary>
    public static FingerprintIdentifyResult NotIdentified(
        FingerprintQualityResult? quality,
        string? deviceId,
        int matchScore)
    {
        return new FingerprintIdentifyResult(
            "fingerprint.identify.result",
            false,
            "Huella no reconocida: no coincide con ninguna huella enrolada.",
            null,
            null,
            0,
            deviceId,
            quality,
            DateTimeOffset.UtcNow)
        {
            Matched = false,
            MatchScore = matchScore
        };
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
