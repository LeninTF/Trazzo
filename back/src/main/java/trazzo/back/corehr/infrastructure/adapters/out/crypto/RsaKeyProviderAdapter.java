package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Component
public class RsaKeyProviderAdapter implements CryptoKeyProviderPort {

    private KeyPair keyPair;
    private String kid;
    private String publicKeyPem;
    private LocalDateTime createdAt;

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
            this.kid = "key-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
            this.publicKeyPem = encodePem(keyPair.getPublic());
            this.createdAt = LocalDateTime.now();
            log.info("RSA-2048 KeyPair generated. kid={}", this.kid);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate RSA KeyPair", e);
        }
    }

    @Override
    public PublicKeyInfo getCurrentPublicKey() {
        return new PublicKeyInfo(publicKeyPem, kid);
    }

    @Override
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    @Override
    public String getCurrentKid() {
        return kid;
    }

    private String encodePem(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }
}
