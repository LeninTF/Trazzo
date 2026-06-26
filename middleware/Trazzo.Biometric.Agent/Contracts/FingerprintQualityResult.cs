namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintQualityResult(
    bool IsAcceptable,
    int ForegroundPixelCount,
    double ForegroundCoveragePercent,
    double ContrastScore,
    bool IsCentered,
    string Message);
