namespace Trazzo.Biometric.Agent.Contracts;

/// <summary>
/// Template cifrado tal como lo espera el backend en <c>/asistencia/marcar</c>,
/// <c>/asistencia/sync</c> y <c>/corehr/biometria/enroll/completar</c>: AES-256-GCM
/// (IV de 12 bytes, tag de 16 bytes, AAD <c>biometric-identify</c>) y la llave AES envuelta
/// con RSA-OAEP SHA-256 / MGF1-SHA-256 usando la clave pública de <c>/security/public-key</c>.
/// </summary>
public sealed record EncryptedPayload(
    string EncryptedTemplateBase64,
    string EncryptedAesKeyBase64,
    string IvBase64,
    string TagBase64);
