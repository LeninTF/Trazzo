package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FeatureTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        Feature f = Feature.create("Biometric", "Fingerprint auth");
        var after = LocalDateTime.now();

        assertNull(f.getId());
        assertEquals("Biometric", f.getName());
        assertEquals("Fingerprint auth", f.getDescription());
        assertFalse(f.getCreatedAt().isBefore(before));
        assertFalse(f.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        Feature f = Feature.restore(5, "GPS", "Location tracking", now, now);

        assertEquals(5, f.getId());
        assertEquals("GPS", f.getName());
        assertEquals("Location tracking", f.getDescription());
        assertEquals(now, f.getCreatedAt());
        assertEquals(now, f.getUpdatedAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_throwsWhenNameBlank(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> Feature.create(name, "description"));
    }

    @Test
    void update_changesNameAndDescription() {
        Feature f = Feature.create("Biometric", "Fingerprint auth");
        f.update("GPS", "Location tracking");
        assertEquals("GPS", f.getName());
        assertEquals("Location tracking", f.getDescription());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void update_throwsWhenNameBlank(String name) {
        Feature f = Feature.create("Biometric", "Fingerprint auth");
        assertThrows(IllegalArgumentException.class, () -> f.update(name, "desc"));
    }

    @Test
    void update_allowsNullDescription() {
        Feature f = Feature.create("Biometric", "Fingerprint auth");
        assertDoesNotThrow(() -> f.update("GPS", null));
        assertNull(f.getDescription());
    }
}
