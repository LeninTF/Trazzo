package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PatchTenantContactRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new PatchTenantContactRequest("email");
        assertThat(r.type()).isEqualTo("email");
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new PatchTenantContactRequest("phone")).isEqualTo(new PatchTenantContactRequest("phone"));
    }
}
