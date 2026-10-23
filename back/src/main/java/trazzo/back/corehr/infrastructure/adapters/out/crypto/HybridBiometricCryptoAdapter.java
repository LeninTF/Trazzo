package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Component
public class HybridBiometricCryptoAdapter implements BiometricCryptoPort {

    private static final byte[] TRANSPORT_AAD = "biometric-identify".getBytes(StandardCharsets.UTF_8);
    private static final int GCM_TAG_BITS = 128;

    private final CryptoKeyProviderPort keyProvider;

    public HybridBiometricCryptoAdapter(CryptoKeyProviderPort keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    public byte[] openTransport(TransportPayload payload) {
        byte[] wrappedKey;
        byte[] iv;
        byte[] ciphertextAndTag;
        try {
            wrappedKey = Base64.getDecoder().decode(payload.encryptedAesKeyBase64());
            iv = Base64.getDecoder().decode(payload.ivBase64());
            byte[] ciphertext = Base64.getDecoder().decode(payload.encryptedTemplateBase64());
            byte[] tag = Base64.getDecoder().decode(payload.tagBase64());
            ciphertextAndTag = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, ciphertextAndTag, 0, ciphertext.length);
            System.arraycopy(tag, 0, ciphertextAndTag, ciphertext.length, tag.length);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BiometricDecryptionException("Payload biométrico con Base64 inválido", e);
        }

        GeneralSecurityException last = null;
        for (PrivateKey key : keyProvider.getDecryptionKeys()) {
            try {
                byte[] aesKey = unwrap(key, wrappedKey);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
                aes.updateAAD(TRANSPORT_AAD);
                return aes.doFinal(ciphertextAndTag);
            } catch (GeneralSecurityException e) {
                last = e;
            }
        }
        throw new BiometricDecryptionException("Error descifrado biométrico: "
                + (last != null ? last.getMessage() : "sin claves de descifrado"), last);
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
}
