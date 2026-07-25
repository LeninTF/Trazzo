package trazzo.back.corehr.application.port.out;

import java.security.PrivateKey;

public interface CryptoKeyProviderPort {

    record PublicKeyInfo(String publicKeyPem, String kid) {}

    PublicKeyInfo getCurrentPublicKey();

    PrivateKey getPrivateKey();

    String getCurrentKid();
}
