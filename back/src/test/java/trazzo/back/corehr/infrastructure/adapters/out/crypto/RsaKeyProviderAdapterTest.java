package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeyProviderAdapterTest {

    private RsaKeyProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RsaKeyProviderAdapter();
        adapter.init();
    }

    @Test
    void getCurrentPublicKey_shouldReturnNonNullPem() {
        var info = adapter.getCurrentPublicKey();

        assertThat(info.publicKeyPem()).isNotBlank();
        assertThat(info.publicKeyPem()).startsWith("-----BEGIN PUBLIC KEY-----");
        assertThat(info.publicKeyPem()).endsWith("-----END PUBLIC KEY-----");
    }

    @Test
    void getCurrentPublicKey_shouldReturnKid() {
        var info = adapter.getCurrentPublicKey();

        assertThat(info.kid()).isNotBlank();
        assertThat(info.kid()).startsWith("key-");
    }

    @Test
    void getPrivateKey_shouldReturnNonNull() {
        assertThat(adapter.getPrivateKey()).isNotNull();
    }

    @Test
    void getCurrentKid_shouldMatchPublicKeyKid() {
        var info = adapter.getCurrentPublicKey();
        var kid = adapter.getCurrentKid();

        assertThat(kid).isEqualTo(info.kid());
    }

    @Test
    void init_shouldGenerateUniqueKidEachTime() throws Exception {
        var adapter2 = new RsaKeyProviderAdapter();
        Thread.sleep(1100);
        adapter2.init();

        assertThat(adapter.getCurrentKid()).isNotEqualTo(adapter2.getCurrentKid());
    }

    @Test
    void getCurrentPublicKey_shouldReturnConsistentData() {
        var info1 = adapter.getCurrentPublicKey();
        var info2 = adapter.getCurrentPublicKey();

        assertThat(info1.publicKeyPem()).isEqualTo(info2.publicKeyPem());
        assertThat(info1.kid()).isEqualTo(info2.kid());
    }
}
