namespace Trazzo.Biometric.Agent.Enrollment;

/// <summary>
/// Huella enrolada guardada localmente en el agente para identificación 1:N on-device.
/// El template es el binario propietario de ZKFinger (el mismo que produce DBMerge) y se
/// persiste cifrado en reposo (DPAPI LocalMachine en Windows).
/// </summary>
public sealed record EnrolledFingerprint(
    string UserReference,
    int FingerIndex,
    byte[] Template);
