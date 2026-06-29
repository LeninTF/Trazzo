package trazzo.back.reports.domain.model.closure;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonthlyClosureDetailTest {

    @Test
    void shouldCreateMonthlyClosureDetailSuccessfully() {

        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        Integer userId = 1;
        LocalDateTime createdAt = LocalDateTime.now();

        MonthlyClosureDetail detail = new MonthlyClosureDetail(
                id,
                closureId,
                userId,
                "Juan Perez",
                "12345678",
                "TI",
                "Developer",
                160.0,
                10,
                1,
                5.0,
                createdAt);

        assertEquals(id, detail.getId());
        assertEquals(closureId, detail.getMonthClosureId());
        assertEquals(userId, detail.getTenantUserId());
        assertEquals("Juan Perez", detail.getTenantUserFullName());
        assertEquals("12345678", detail.getTenantUserDocument());
        assertEquals("TI", detail.getDepartmentName());
        assertEquals("Developer", detail.getRoleName());
        assertEquals(160.0, detail.getTotalWorkedHours());
        assertEquals(Integer.valueOf(10), detail.getTotalTardinessMinutes());
        assertEquals(1, detail.getTotalAbsences());
        assertEquals(5.0, detail.getTotalOvertimeHours());
        assertEquals(createdAt, detail.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenWorkedHoursIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(-1.0, 10, 0, 5.0));
    }

    @Test
    void shouldThrowExceptionWhenWorkedHoursIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(null, 10, 0, 5.0));
    }

    @Test
    void shouldThrowExceptionWhenTardinessMinutesIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(10.0, -1, 0, 5.0));
    }

    @Test
    void shouldThrowExceptionWhenTardinessMinutesIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(10.0, null, 0, 5.0));
    }

    @Test
    void shouldThrowExceptionWhenOvertimeHoursIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(10.0, 10, 0, -1.0));
    }

    @Test
    void shouldThrowExceptionWhenOvertimeHoursIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(10.0, 10, 0, null));
    }

    @Test
    void shouldThrowExceptionWhenAbsencesIsNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> createDetail(10.0, 10, -1, 1.0));
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        UUID monthClosureId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        assertThrows(NullPointerException.class,
                () -> new MonthlyClosureDetail(
                        null,
                        monthClosureId,
                        1,
                        "Juan",
                        "123",
                        "TI",
                        "Dev",
                        10.0,
                        10,
                        0,
                        1.0,
                        createdAt));
    }

    private MonthlyClosureDetail createDetail(
            Double workedHours,
            Integer tardiness,
            int absences,
            Double overtime) {

        return new MonthlyClosureDetail(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2,
                "Juan Perez",
                "12345678",
                "TI",
                "Developer",
                workedHours,
                tardiness,
                absences,
                overtime,
                LocalDateTime.now());
    }
}