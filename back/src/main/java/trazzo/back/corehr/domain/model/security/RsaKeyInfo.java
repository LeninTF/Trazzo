package trazzo.back.corehr.domain.model.security;

import java.time.LocalDateTime;

public record RsaKeyInfo(String publicKeyPem, String kid, LocalDateTime createdAt) {
}
