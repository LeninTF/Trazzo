namespace Trazzo.Biometric.Agent.Contracts;

public sealed record EncryptedPayload(
    string EncryptedTemplateBase64,
    string EncryptedAesKeyBase64,
    string IvBase64,
    string TagBase64)
{
    // El backend expone un único campo `template_cifrado` (sin iv/tag por separado).
    // Empaquetamos en el formato AES-GCM estándar `iv || cipher || tag` para no perder
    // la info necesaria cuando el backend implemente el descifrado. Esto es lo que
    // conviene el ecosistema .NET/Java al intercambiar payloads AES-GCM.
    public string ToPackedTemplateBase64()
    {
        byte[] iv = Convert.FromBase64String(IvBase64);
        byte[] cipher = Convert.FromBase64String(EncryptedTemplateBase64);
        byte[] tag = Convert.FromBase64String(TagBase64);

        byte[] packed = new byte[iv.Length + cipher.Length + tag.Length];
        Buffer.BlockCopy(iv, 0, packed, 0, iv.Length);
        Buffer.BlockCopy(cipher, 0, packed, iv.Length, cipher.Length);
        Buffer.BlockCopy(tag, 0, packed, iv.Length + cipher.Length, tag.Length);
        return Convert.ToBase64String(packed);
    }
}
