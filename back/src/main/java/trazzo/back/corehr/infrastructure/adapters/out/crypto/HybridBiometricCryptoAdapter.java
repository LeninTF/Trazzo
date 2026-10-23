package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Base64;

@Component
public class HybridBiometricCryptoAdapter implements BiometricCryptoPort {

    static final String STORAGE_KEY_PROPERTY = "app.biometric.storage-key";

    private static final byte[] TRANSPORT_AAD = "biometric-identify".getBytes(StandardCharsets.UTF_8);
    private static final byte[] STORAGE_AAD = "biometric-storage:v1".getBytes(StandardCharsets.UTF_8);
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_TAG_BYTES = GCM_TAG_BITS / 8;
    private static final int GCM_IV_BYTES = 12;

    private final CryptoKeyProviderPort keyProvider;
    private final SecretKeySpec storageKey;
    private final SecureRandom random = new SecureRandom();

    // Sin app.biometric.storage-key se usa app.encryption.key. El fallback va aquí y no en un placeholder
    // ${A:${B}}: "APP_BIOMETRIC_STORAGE_KEY=" en el .env deja la propiedad vacía, no ausente.
    public HybridBiometricCryptoAdapter(
            CryptoKeyProviderPort keyProvider,
            @Value("${" + STORAGE_KEY_PROPERTY + ":}") String storageKeyBase64,
            @Value("${app.encryption.key:}") String encryptionKeyBase64) {
        this.keyProvider = keyProvider;
        this.storageKey = parseStorageKey(isBlank(storageKeyBase64) ? encryptionKeyBase64 : storageKeyBase64);
    }

    @Override
    public byte[] openTransport(TransportPayload payload) {
        byte[] wrappedKey;
        byte[] iv;
        byte[] ciphertextAndTag;
        try {
            wrappedKey = Base64.getDecoder().decode(payload.encryptedAesKeyBase64());
            iv = Base64.getDecoder().decode(payload.ivBase64());
            ciphertextAndTag = concat(Base64.getDecoder().decode(payload.encryptedTemplateBase64()),
                    Base64.getDecoder().decode(payload.tagBase64()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BiometricDecryptionException("Payload biométrico con Base64 inválido", e);
        }

        GeneralSecurityException last = null;
        for (PrivateKey key : keyProvider.getDecryptionKeys()) {
            try {
                byte[] aesKey = unwrap(key, wrappedKey);
                return gcmDecrypt(new SecretKeySpec(aesKey, "AES"), iv, TRANSPORT_AAD, ciphertextAndTag);
            } catch (GeneralSecurityException e) {
                last = e;
            }
        }
        throw new BiometricDecryptionException("Error descifrado biométrico: "
                + (last != null ? last.getMessage() : "sin claves de descifrado"), last);
    }

    @Override
    public StoredTemplate seal(byte[] template) {
        if (storageKey == null) {
            throw new IllegalStateException(STORAGE_KEY_PROPERTY + " (o app.encryption.key) no está configurada: "
                    + "no se pueden guardar huellas");
        }
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, storageKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            aes.updateAAD(STORAGE_AAD);
            byte[] sealed = aes.doFinal(template);
            int ciphertextLength = sealed.length - GCM_TAG_BYTES;
            Base64.Encoder base64 = Base64.getEncoder();
            return new StoredTemplate(
                    base64.encodeToString(Arrays.copyOfRange(sealed, 0, ciphertextLength)),
                    base64.encodeToString(iv),
                    base64.encodeToString(Arrays.copyOfRange(sealed, ciphertextLength, sealed.length)),
                    STORAGE_KEY_REF);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo sellar el template biométrico", e);
        }
    }

    @Override
    public byte[] open(UserBiometria enrolled) {
        if (enrolled.getKeyRef() == null) {
            return openTransport(new TransportPayload(
                    enrolled.getEncryptedTemplateBase64(),
                    enrolled.getEncryptedAesKeyBase64(),
                    enrolled.getIvBase64(),
                    enrolled.getTagBase64()));
        }
        if (!STORAGE_KEY_REF.equals(enrolled.getKeyRef())) {
            throw new BiometricDecryptionException("keyRef desconocido: " + enrolled.getKeyRef(), null);
        }
        if (storageKey == null) {
            throw new BiometricDecryptionException(STORAGE_KEY_PROPERTY + " no está configurada", null);
        }
        byte[] iv;
        byte[] ciphertextAndTag;
        try {
            iv = Base64.getDecoder().decode(enrolled.getIvBase64());
            ciphertextAndTag = concat(Base64.getDecoder().decode(enrolled.getEncryptedTemplateBase64()),
                    Base64.getDecoder().decode(enrolled.getTagBase64()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BiometricDecryptionException("Template guardado con Base64 inválido", e);
        }
        try {
            return gcmDecrypt(storageKey, iv, STORAGE_AAD, ciphertextAndTag);
        } catch (GeneralSecurityException e) {
            throw new BiometricDecryptionException("Error descifrado biométrico: " + e.getMessage(), e);
        }
    }

    // Los parámetros van explícitos: con solo la transformación, SunJCE usa MGF1-SHA1 y no
    // interopera con RSAEncryptionPadding.OaepSHA256 de .NET, que usa MGF1-SHA256.
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private static byte[] unwrap(PrivateKey key, byte[] wrappedKey) throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, key, OAEP_SHA256);
        return rsa.doFinal(wrappedKey);
    }

    private static byte[] gcmDecrypt(SecretKeySpec key, byte[] iv, byte[] aad, byte[] ciphertextAndTag)
            throws GeneralSecurityException {
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        aes.updateAAD(aad);
        return aes.doFinal(ciphertextAndTag);
    }

    private static byte[] concat(byte[] ciphertext, byte[] tag) {
        byte[] joined = Arrays.copyOf(ciphertext, ciphertext.length + tag.length);
        System.arraycopy(tag, 0, joined, ciphertext.length, tag.length);
        return joined;
    }

    // El mensaje nombra la propiedad, nunca el valor.
    private static SecretKeySpec parseStorageKey(String base64) {
        if (isBlank(base64)) {
            return null;
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(STORAGE_KEY_PROPERTY + " (o app.encryption.key) no es Base64 válido");
        }
        if (key.length != 32) {
            throw new IllegalStateException(STORAGE_KEY_PROPERTY + " (o app.encryption.key) debe ser Base64 de 32 bytes (AES-256)");
        }
        return new SecretKeySpec(key, "AES");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
