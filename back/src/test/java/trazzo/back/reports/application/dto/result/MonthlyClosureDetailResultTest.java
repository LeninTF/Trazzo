package trazzo.back.reports.application.dto.result;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureDetailResultTest {

    @Test
    void shouldCreateResultSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID monthClosureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResult result = new MonthlyClosureDetailResult(
                id, monthClosureId, "Juan Perez", "12345678", "TI", "Developer",
                160.0, 10.0, 1, 5.0, now);

        assertEquals(id, result.id());
        assertEquals(monthClosureId, result.monthClosureId());
        assertEquals("Juan Perez", result.tenantUserFullName());
        assertEquals("12345678", result.tenantUserDocument());
        assertEquals("TI", result.departmentName());
        assertEquals("Developer", result.roleName());
        assertEquals(160.0, result.totalWorkedHours());
        assertEquals(10.0, result.totalTardinessMinutes());
        assertEquals(1, result.totalAbsences());
        assertEquals(5.0, result.totalOvertimeHours());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        UUID monthClosureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResult r1 = new MonthlyClosureDetailResult(
                id, monthClosureId, "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0, now);
        MonthlyClosureDetailResult r2 = new MonthlyClosureDetailResult(
                id, monthClosureId, "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0, now);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureDetailResult r1 = new MonthlyClosureDetailResult(
                UUID.randomUUID(), UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0, LocalDateTime.now());
        MonthlyClosureDetailResult r2 = new MonthlyClosureDetailResult(
                UUID.randomUUID(), UUID.randomUUID(), "Ana", "456", "HR", "Mgr", 80.0, 5.0, 0, 2.0, LocalDateTime.now());


        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        UUID id = UUID.randomUUID();
        MonthlyClosureDetailResult result = new MonthlyClosureDetailResult(
                id, UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0, LocalDateTime.now());

        assertNotNull(result.toString());
        assertTrue(result.toString().contains(id.toString()));
    }
}
