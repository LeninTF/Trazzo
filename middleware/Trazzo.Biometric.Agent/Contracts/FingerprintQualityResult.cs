namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintQualityResult(
    bool IsAcceptable,
    int ForegroundPixelCount,
    double ForegroundCoveragePercent,
    double ContrastScore,
    bool IsCentered,
    string Message)
{
    public double ScorePercent { get; init; }

    /// <summary>
    /// Coherencia direccional (0-100) de la superficie capturada. Alta en huellas reales
    /// (crestas orientadas), baja en codo/palma/objeto sin crestas. 0 cuando el análisis
    /// de estructura está desactivado (<c>Biometric:Quality:MinimumRidgeCoherencePercent = 0</c>).
    /// </summary>
    public double RidgeCoherencePercent { get; init; }
}
