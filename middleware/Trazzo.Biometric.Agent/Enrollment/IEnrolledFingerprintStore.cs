namespace Trazzo.Biometric.Agent.Enrollment;

/// <summary>
/// Padrón local de huellas enroladas del agente. Alimenta la identificación 1:N on-device
/// (ZKFinger DBAdd + DBIdentify), que es la que rechaza superficies no enroladas (p. ej. la
/// palma de la mano): si la captura no coincide con ninguna huella del padrón, no se identifica.
/// </summary>
public interface IEnrolledFingerprintStore
{
    /// <summary>Guarda (o reemplaza) la huella enrolada de un usuario/dedo.</summary>
    Task SaveAsync(EnrolledFingerprint fingerprint, CancellationToken cancellationToken = default);

    /// <summary>Devuelve todas las huellas enroladas para cargarlas en el motor del SDK.</summary>
    Task<IReadOnlyList<EnrolledFingerprint>> GetAllAsync(CancellationToken cancellationToken = default);
}
