package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CreateTenantContactRequestTest {
    @Test
    void constructorAndGetters() {
        var r = new CreateTenantContactRequest(10L, "email");
        assertThat(r.tenantUserId()).isEqualTo(10L);
        assertThat(r.type()).isEqualTo("email");
    }
    @Test
    void equalsAndHashCode() {
        assertThat(new CreateTenantContactRequest(1L, "phone"))
                .isEqualTo(new CreateTenantContactRequest(1L, "phone"))
                .hasSameHashCodeAs(new CreateTenantContactRequest(1L, "phone"));
    }
}
