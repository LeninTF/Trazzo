using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Services;

public interface IBiometricScannerService : IAsyncDisposable
{
    Task InitializeAsync(CancellationToken cancellationToken);

    Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken);

    Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken);

    Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken);

    Task<FingerprintMatchResult> MatchFingerprintAgainstTemplatesAsync(
        IReadOnlyList<(int Index, byte[] Template)> templates,
        CancellationToken cancellationToken);

    Task<FingerprintEnrollResult> EnrollFingerprintAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken cancellationToken);

    FingerprintEnrollResult CancelEnrollment();
}
