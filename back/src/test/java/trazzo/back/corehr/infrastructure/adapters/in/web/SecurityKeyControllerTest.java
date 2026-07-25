package trazzo.back.corehr.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityKeyController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityKeyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CryptoKeyProviderPort cryptoKeyProvider;

    private static final String PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqh...\n-----END PUBLIC KEY-----";
    private static final String KID = "key-2025-01-15-103000";

    @Test
    void getPublicKey_shouldReturn200() throws Exception {
        when(cryptoKeyProvider.getCurrentPublicKey())
                .thenReturn(new CryptoKeyProviderPort.PublicKeyInfo(PUBLIC_KEY_PEM, KID));

        mockMvc.perform(get("/security/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value(PUBLIC_KEY_PEM))
                .andExpect(jsonPath("$.kid").value(KID))
                .andExpect(header().string("X-RateLimit-Limit", "60"));
    }

    @Test
    void getPublicKey_shouldHaveCacheControl() throws Exception {
        when(cryptoKeyProvider.getCurrentPublicKey())
                .thenReturn(new CryptoKeyProviderPort.PublicKeyInfo(PUBLIC_KEY_PEM, KID));

        mockMvc.perform(get("/security/public-key"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    void getPublicKey_shouldHaveETag() throws Exception {
        when(cryptoKeyProvider.getCurrentPublicKey())
                .thenReturn(new CryptoKeyProviderPort.PublicKeyInfo(PUBLIC_KEY_PEM, KID));

        mockMvc.perform(get("/security/public-key"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void getPublicKey_shouldReturnCorrectJsonStructure() throws Exception {
        when(cryptoKeyProvider.getCurrentPublicKey())
                .thenReturn(new CryptoKeyProviderPort.PublicKeyInfo(PUBLIC_KEY_PEM, KID));

        mockMvc.perform(get("/security/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").isString())
                .andExpect(jsonPath("$.kid").isString())
                .andExpect(jsonPath("$.nonExistentField").doesNotExist());
    }
}
