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

    /// <summary>
    /// Enrola una huella (3 capturas + DBMerge). Si se indica <paramref name="userReference"/>,
    /// la huella se guarda en el padrón local y se agrega al motor de identificación 1:N (DBAdd),
    /// para que futuras identificaciones puedan reconocerla.
    /// </summary>
    Task<FingerprintEnrollResult> EnrollFingerprintAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken cancellationToken,
        string? userReference = null,
        int fingerIndex = 0);

    FingerprintEnrollResult CancelEnrollment();
}
