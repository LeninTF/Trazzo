package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureDetailResponseTest {

    @Test
    void shouldCreateResponseSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResponse response = new MonthlyClosureDetailResponse(
                id, closureId, 1, "Juan Perez", "12345678", "TI", "Developer",
                160.0, 10, 1, 5.0, now);
        assertEquals(id, response.id());
        assertEquals(closureId, response.monthClosureId());
        assertEquals(Integer.valueOf(1), response.tenantUserId());
        assertEquals("Juan Perez", response.tenantUserFullName());
        assertEquals("12345678", response.tenantUserDocument());
        assertEquals("TI", response.departmentName());
        assertEquals("Developer", response.roleName());
        assertEquals(160.0, response.totalWorkedHours());
        assertEquals(Integer.valueOf(10), response.totalTardinessMinutes());
        assertEquals(1, response.totalAbsences());
        assertEquals(5.0, response.totalOvertimeHours());
        assertEquals(now, response.createdAt());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResponse r1 = new MonthlyClosureDetailResponse(
                id, closureId, 1, "Juan", "123", "TI", "Dev", 160.0, 10, 1, 5.0, now);
        MonthlyClosureDetailResponse r2 = new MonthlyClosureDetailResponse(
                id, closureId, 1, "Juan", "123", "TI", "Dev", 160.0, 10, 1, 5.0, now);
        assertEquals(r1, r2);
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureDetailResponse r1 = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), 1, "Juan", "123", "TI", "Dev", 160.0, 10, 1, 5.0, LocalDateTime.now());
        MonthlyClosureDetailResponse r2 = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), 2, "Ana", "456", "HR", "Mgr", 80.0, 5, 0, 2.0, LocalDateTime.now());
        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        MonthlyClosureDetailResponse response = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), 1, "Juan", "123", "TI", "Dev", 160.0, 10, 1, 5.0, LocalDateTime.now());
        assertNotNull(response.toString());
    }
}
