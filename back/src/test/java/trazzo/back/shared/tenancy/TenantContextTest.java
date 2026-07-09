package trazzo.back.shared.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void get_returnsDefaultSchemaWhenNothingSet() {
        assertEquals("public", TenantContext.get());
    }

    @Test
    void set_thenGet_returnsTheSchema() {
        TenantContext.set("tenant_acme");
        assertEquals("tenant_acme", TenantContext.get());
    }

    @Test
    void set_nullFallsBackToDefaultSchema() {
        TenantContext.set(null);
        assertEquals("public", TenantContext.get());
    }

    @Test
    void set_blankFallsBackToDefaultSchema() {
        TenantContext.set("   ");
        assertEquals("public", TenantContext.get());
    }

    @Test
    void clear_resetsToDefaultSchema() {
        TenantContext.set("tenant_acme");
        TenantContext.clear();
        assertEquals("public", TenantContext.get());
    }

    @Test
    void set_isIsolatedPerThread() throws InterruptedException {
        TenantContext.set("tenant_main");
        String[] otherThreadValue = new String[1];

        Thread other = new Thread(() -> {
            otherThreadValue[0] = TenantContext.get();
            TenantContext.set("tenant_other");
            TenantContext.clear();
        });
        other.start();
        other.join();

        assertEquals("public", otherThreadValue[0]);
        assertEquals("tenant_main", TenantContext.get());
    }
}
