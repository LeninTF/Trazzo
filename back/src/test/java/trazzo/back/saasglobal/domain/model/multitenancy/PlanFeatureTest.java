package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;

class PlanFeatureTest {

    private static final LocalDate START = LocalDate.of(2025, Month.JANUARY, 1);

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        PlanFeature pf = PlanFeature.create(1, 2, "BOOLEAN", "true", START);
        var after = LocalDateTime.now();

        assertNull(pf.getId());
        assertEquals(1, pf.getPlanId());
        assertEquals(2, pf.getFeatureId());
        assertEquals("BOOLEAN", pf.getDataType());
        assertEquals("true", pf.getValue());
        assertEquals(START, pf.getDateStart());
        assertNull(pf.getDateEnd());
        assertTrue(pf.isActive());
        assertFalse(pf.getCreatedAt().isBefore(before));
        assertFalse(pf.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var end = LocalDate.of(2025, Month.DECEMBER, 31);
        PlanFeature pf = PlanFeature.restore(3, 1, 2, "NUMBER", "100", START, end, false, now, now);

        assertEquals(3, pf.getId());
        assertEquals("NUMBER", pf.getDataType());
        assertEquals("100", pf.getValue());
        assertEquals(end, pf.getDateEnd());
        assertFalse(pf.isActive());
    }

    @Test
    void create_throwsWhenPlanIdNull() {
        assertThrows(IllegalArgumentException.class,
                () -> PlanFeature.create(null, 2, "BOOLEAN", "true", START));
    }

    @Test
    void create_throwsWhenFeatureIdNull() {
        assertThrows(IllegalArgumentException.class,
                () -> PlanFeature.create(1, null, "BOOLEAN", "true", START));
    }

    @Test
    void create_throwsWhenDataTypeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> PlanFeature.create(1, 2, "", "true", START));
    }

    @Test
    void create_throwsWhenValueBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> PlanFeature.create(1, 2, "BOOLEAN", " ", START));
    }
}
