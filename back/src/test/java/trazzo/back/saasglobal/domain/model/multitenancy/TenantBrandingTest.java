package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TenantBrandingTest {

    @Test
    void of_setsAllFields() {
        var before = LocalDateTime.now();
        var b = TenantBranding.of("t-1", "http://logo.png", "slogan", "#fff", "#000");
        var after = LocalDateTime.now();

        assertEquals("t-1", b.getTenantId());
        assertEquals("http://logo.png", b.getLogoUrl());
        assertEquals("slogan", b.getSlogan());
        assertEquals("#fff", b.getPrimaryColor());
        assertEquals("#000", b.getSecondaryColor());
        assertFalse(b.getCreatedAt().isBefore(before));
        assertFalse(b.getCreatedAt().isAfter(after));
    }

    @Test
    void of_nullFieldsAreAllowed() {
        assertDoesNotThrow(() -> TenantBranding.of(null, null, null, null, null));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var earlier = now.minusDays(1);
        var b = TenantBranding.restore("t-2", "logo", "s", "#aaa", "#bbb", earlier, now);

        assertEquals("t-2", b.getTenantId());
        assertEquals("logo", b.getLogoUrl());
        assertEquals("s", b.getSlogan());
        assertEquals("#aaa", b.getPrimaryColor());
        assertEquals("#bbb", b.getSecondaryColor());
        assertEquals(earlier, b.getCreatedAt());
        assertEquals(now, b.getUpdatedAt());
    }

    @Test
    void update_changesAllFields() {
        var b = TenantBranding.of("t-1", "old-logo", "old-slogan", "#000", "#fff");
        b.update("new-logo", "new-slogan", "#111", "#222");

        assertEquals("new-logo", b.getLogoUrl());
        assertEquals("new-slogan", b.getSlogan());
        assertEquals("#111", b.getPrimaryColor());
        assertEquals("#222", b.getSecondaryColor());
    }

    @Test
    void update_setsUpdatedAt() {
        var b = TenantBranding.of("t-1", "logo", "s", "#a", "#b");
        var before = LocalDateTime.now();
        b.update("new", "new", "#c", "#d");
        var after = LocalDateTime.now();

        assertFalse(b.getUpdatedAt().isBefore(before));
        assertFalse(b.getUpdatedAt().isAfter(after));
    }
}
