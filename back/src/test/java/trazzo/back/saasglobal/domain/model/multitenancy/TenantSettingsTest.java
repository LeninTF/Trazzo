package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;

class TenantSettingsTest {

    @Test
    void of_setsAllFields() {
        var before = LocalDateTime.now();
        var s = TenantSettings.of("t-1", "tenant_demo");
        var after = LocalDateTime.now();

        assertEquals("t-1", s.getTenantId());
        assertEquals("tenant_demo", s.getSchemaName());
        assertFalse(s.getCreatedAt().isBefore(before));
        assertFalse(s.getCreatedAt().isAfter(after));
    }

    @Test
    void of_nullTenantIdIsAllowed() {
        assertDoesNotThrow(() -> TenantSettings.of(null, "tenant_demo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void of_throwsWhenSchemaNameBlank(String schemaName) {
        assertThrows(IllegalArgumentException.class,
                () -> TenantSettings.of("t-1", schemaName));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var s = TenantSettings.restore("t-2", "tenant_demo", now, now);

        assertEquals("t-2", s.getTenantId());
        assertEquals("tenant_demo", s.getSchemaName());
        assertEquals(now, s.getCreatedAt());
        assertEquals(now, s.getUpdatedAt());
    }

    @Test
    void deriveSchemaName_sanitizesAndPrefixes() {
        assertEquals("tenant_universidad_abc", TenantSettings.deriveSchemaName("Universidad-ABC"));
    }

    @Test
    void deriveSchemaName_throwsWhenSubDomainTooLongToDeriveUniqueName() {
        // Truncating would let two distinct, unique subdomains collide on the same schema
        // name — reject instead of silently truncating.
        String longSubDomain = "a".repeat(100);
        assertThrows(TenantValidationException.class,
                () -> TenantSettings.deriveSchemaName(longSubDomain));
    }

    @Test
    void deriveSchemaName_allowsSubDomainAtExactMaxLength() {
        String subDomain = "a".repeat(55);
        String schemaName = TenantSettings.deriveSchemaName(subDomain);

        assertEquals("tenant_" + subDomain, schemaName);
    }
}
