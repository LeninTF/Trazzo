package trazzo.back.corehr.application.port.out;

import java.security.PrivateKey;
import java.util.List;

public interface CryptoKeyProviderPort {

    record PublicKeyInfo(String publicKeyPem, String kid) {}

    PublicKeyInfo getCurrentPublicKey();

    PrivateKey getPrivateKey();

    /** Claves con las que se intenta descifrar, la actual primero (las anteriores permiten rotar). */
    default List<PrivateKey> getDecryptionKeys() {
        return List.of(getPrivateKey());
    }

    String getCurrentKid();
}
