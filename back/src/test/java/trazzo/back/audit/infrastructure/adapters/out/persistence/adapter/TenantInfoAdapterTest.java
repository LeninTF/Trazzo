package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantInfoAdapterTest {

    private final TenantInfoAdapter adapter = new TenantInfoAdapter();

    @Test
    void findByTenantIdReturnsEmpty() {
        var result = adapter.findByTenantId("any-tenant");
        assertThat(result).isEmpty();
    }
}
