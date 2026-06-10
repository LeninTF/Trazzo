namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintEnrollProgress(
    string Type,
    int Step,
    int TotalSteps,
    string Message)
{
    public static FingerprintEnrollProgress Create(int step, int totalSteps, string message)
    {
        return new FingerprintEnrollProgress("fingerprint.enroll.progress", step, totalSteps, message);
    }
}
