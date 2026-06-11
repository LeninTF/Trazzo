namespace Trazzo.Biometric.Agent.Contracts;

public sealed record EncryptedPayload(
    string EncryptedTemplateBase64,
    string EncryptedAesKeyBase64,
    string IvBase64,
    string TagBase64);
