package trazzo.back.corehr.application.port.out;

/**
 * Criptografía de templates biométricos. El transporte es el contrato con el middleware: la llave
 * AES-256 viaja envuelta con RSA-OAEP SHA-256/MGF1-SHA-256 y el template cifrado con AES-256-GCM
 * (IV 12 B, tag 16 B, AAD "biometric-identify").
 */
public interface BiometricCryptoPort {

    record TransportPayload(String encryptedTemplateBase64, String encryptedAesKeyBase64,
                            String ivBase64, String tagBase64) {}

    /** Descifra un payload del middleware; lanza BiometricDecryptionException si no es válido. */
    byte[] openTransport(TransportPayload payload);
}
