package trazzo.back.reports.application.dto.result;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class MonthlyClosureDetailResultTest {

    @Test
    void shouldCreateResultSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID monthClosureId = UUID.randomUUID();
        MonthlyClosureDetailResult result = new MonthlyClosureDetailResult(
                id, monthClosureId, "Juan Perez", "12345678", "TI", "Developer",
                160.0, 10.0, 1, 5.0);

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
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        UUID monthClosureId = UUID.randomUUID();
        MonthlyClosureDetailResult r1 = new MonthlyClosureDetailResult(
                id, monthClosureId, "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);
        MonthlyClosureDetailResult r2 = new MonthlyClosureDetailResult(
                id, monthClosureId, "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureDetailResult r1 = new MonthlyClosureDetailResult(
                UUID.randomUUID(), UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);
        MonthlyClosureDetailResult r2 = new MonthlyClosureDetailResult(
                UUID.randomUUID(), UUID.randomUUID(), "Ana", "456", "HR", "Mgr", 80.0, 5.0, 0, 2.0);


        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        UUID id = UUID.randomUUID();
        MonthlyClosureDetailResult result = new MonthlyClosureDetailResult(
                id, UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);

        assertNotNull(result.toString());
        assertTrue(result.toString().contains(id.toString()));
    }
}
