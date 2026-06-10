using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Services;

public interface IBiometricScannerService : IAsyncDisposable
{
    Task InitializeAsync(CancellationToken cancellationToken);

    Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken);

    Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken);

    Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken)
    {
        return Task.FromResult(FingerprintIdentifyResult.Failed("No se pudo capturar la huella para identificación."));
    }

    Task<FingerprintEnrollResult> EnrollFingerprintAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
        CancellationToken cancellationToken)
    {
        return Task.FromResult(FingerprintEnrollResult.Failed("No se pudo enrolar la huella. Intente nuevamente."));
    }

    FingerprintEnrollResult CancelEnrollment()
    {
        return FingerprintEnrollResult.Failed("No hay un enrolamiento activo.");
    }
}
