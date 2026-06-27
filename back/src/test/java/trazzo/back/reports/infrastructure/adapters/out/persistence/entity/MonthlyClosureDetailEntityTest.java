package trazzo.back.reports.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureDetailEntityTest {

    @Test
    void shouldCreateEntitySuccessfully() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailEntity entity = new MonthlyClosureDetailEntity(
                id, closureId, "user-1", "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10.0, 1, 5.0, now);
        assertEquals(id, entity.getId());
        assertEquals(closureId, entity.getMonthClosureId());
        assertEquals("user-1", entity.getTenantUserId());
        assertEquals("Juan Perez", entity.getTenantUserFullName());
        assertEquals("12345678", entity.getTenantUserDocument());
        assertEquals("TI", entity.getDepartmentName());
        assertEquals("Developer", entity.getRoleName());
        assertEquals(160.0, entity.getTotalWorkedHours());
        assertEquals(10.0, entity.getTotalTardinessMinutes());
        assertEquals(1, entity.getTotalAbsences());
        assertEquals(5.0, entity.getTotalOvertimeHours());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldSupportDefaultConstructor() {
        MonthlyClosureDetailEntity entity = new MonthlyClosureDetailEntity();
        assertNull(entity.getId());
    }

    @Test
    void shouldSupportSetters() {
        MonthlyClosureDetailEntity entity = new MonthlyClosureDetailEntity();
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        entity.setId(id);
        entity.setMonthClosureId(closureId);
        entity.setTenantUserId("user-1");
        entity.setTenantUserFullName("Juan Perez");
        entity.setTenantUserDocument("12345678");
        entity.setDepartmentName("TI");
        entity.setRoleName("Developer");
        entity.setTotalWorkedHours(160.0);
        entity.setTotalTardinessMinutes(10.0);
        entity.setTotalAbsences(1);
        entity.setTotalOvertimeHours(5.0);
        entity.setCreatedAt(now);
        assertEquals(id, entity.getId());
        assertEquals(closureId, entity.getMonthClosureId());
        assertEquals("user-1", entity.getTenantUserId());
        assertEquals("Juan Perez", entity.getTenantUserFullName());
        assertEquals("12345678", entity.getTenantUserDocument());
        assertEquals("TI", entity.getDepartmentName());
        assertEquals("Developer", entity.getRoleName());
        assertEquals(160.0, entity.getTotalWorkedHours());
        assertEquals(10.0, entity.getTotalTardinessMinutes());
        assertEquals(1, entity.getTotalAbsences());
        assertEquals(5.0, entity.getTotalOvertimeHours());
        assertEquals(now, entity.getCreatedAt());
    }
}
