package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class MonthlyClosureDetailResponseTest {

    @Test
    void shouldCreateResponseSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        MonthlyClosureDetailResponse response = new MonthlyClosureDetailResponse(
                id, closureId, "Juan Perez", "12345678", "TI", "Developer",
                160.0, 10.0, 1, 5.0);
        assertEquals(id, response.id());
        assertEquals(closureId, response.monthClosureId());
        assertEquals("Juan Perez", response.tenantUserFullName());
        assertEquals("12345678", response.tenantUserDocument());
        assertEquals("TI", response.departmentName());
        assertEquals("Developer", response.roleName());
        assertEquals(160.0, response.totalWorkedHours());
        assertEquals(10.0, response.totalTardinessMinutes());
        assertEquals(1, response.totalAbsences());
        assertEquals(5.0, response.totalOvertimeHours());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        MonthlyClosureDetailResponse r1 = new MonthlyClosureDetailResponse(
                id, closureId, "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);
        MonthlyClosureDetailResponse r2 = new MonthlyClosureDetailResponse(
                id, closureId, "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);
        assertEquals(r1, r2);
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureDetailResponse r1 = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);
        MonthlyClosureDetailResponse r2 = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Ana", "456", "HR", "Mgr", 80.0, 5.0, 0, 2.0);
        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        MonthlyClosureDetailResponse response = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0);
        assertNotNull(response.toString());
    }
}
