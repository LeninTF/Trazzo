package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.attendance.UserBiometria;

/**
 * Criptografía de templates biométricos. El transporte es el contrato con el middleware: la llave
 * AES-256 viaja envuelta con RSA-OAEP SHA-256/MGF1-SHA-256 y el template cifrado con AES-256-GCM
 * (IV 12 B, tag 16 B, AAD "biometric-identify"). Al enrolar, el template se vuelve a sellar con la
 * clave de almacenamiento (AES-256-GCM, AAD "biometric-storage:v1") para que no dependa de la clave RSA.
 */
public interface BiometricCryptoPort {

    String STORAGE_KEY_REF = "storage-v1";

    record TransportPayload(String encryptedTemplateBase64, String encryptedAesKeyBase64,
                            String ivBase64, String tagBase64) {}

    record StoredTemplate(String ciphertextBase64, String ivBase64, String tagBase64, String keyRef) {}

    /** Descifra un payload del middleware; lanza BiometricDecryptionException si no es válido. */
    byte[] openTransport(TransportPayload payload);

    /** Sella un template en claro con la clave de almacenamiento. */
    StoredTemplate seal(byte[] template);

    /**
     * Abre una fila guardada: con {@code keyRef} usa la clave de almacenamiento; sin él es un payload
     * de transporte guardado antes del re-sellado y se abre con las claves RSA.
     */
    byte[] open(UserBiometria enrolled);
}
