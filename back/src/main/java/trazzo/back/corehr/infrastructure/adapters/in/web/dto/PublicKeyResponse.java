package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;

public record PublicKeyResponse(
        @JsonProperty("publicKey") String publicKey,
        @JsonProperty("kid") String kid
) {
    public static PublicKeyResponse from(CryptoKeyProviderPort.PublicKeyInfo info) {
        return new PublicKeyResponse(info.publicKeyPem(), info.kid());
    }
}
