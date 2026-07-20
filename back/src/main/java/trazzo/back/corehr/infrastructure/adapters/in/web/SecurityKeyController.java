package trazzo.back.corehr.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PublicKeyResponse;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
public class SecurityKeyController {

    private final CryptoKeyProviderPort cryptoKeyProvider;

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey() {
        var keyInfo = cryptoKeyProvider.getCurrentPublicKey();
        var response = PublicKeyResponse.from(keyInfo);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .eTag(keyInfo.kid())
                .header("X-RateLimit-Limit", "60")
                .body(response);
    }
}
