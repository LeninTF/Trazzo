namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintMatchResult(
    string Type,
    bool Success,
    string Message,
    bool Matched,
    int? MatchedIndex,
    int TemplateSize,
    FingerprintQualityResult? Quality,
    DateTimeOffset CapturedAtUtc,
    int TemplatesProvided)
{
    public static FingerprintMatchResult MatchedResult(
        int matchedIndex,
        int templateSize,
        FingerprintQualityResult? quality,
        int templatesProvided)
    {
        return new FingerprintMatchResult(
            "fingerprint.match.result",
            true,
            "Huella verificada y coincidente.",
            true,
            matchedIndex,
            templateSize,
            quality,
            DateTimeOffset.UtcNow,
            templatesProvided);
    }

    public static FingerprintMatchResult NoMatchResult(
        int templateSize,
        FingerprintQualityResult? quality,
        int templatesProvided)
    {
        return new FingerprintMatchResult(
            "fingerprint.match.result",
            true,
            "Huella capturada pero no coincide con ningún template enrolado.",
            false,
            null,
            templateSize,
            quality,
            DateTimeOffset.UtcNow,
            templatesProvided);
    }

    public static FingerprintMatchResult Failed(string message)
    {
        return new FingerprintMatchResult(
            "fingerprint.match.result",
            false,
            message,
            false,
            null,
            0,
            null,
            DateTimeOffset.UtcNow,
            0);
    }
}
