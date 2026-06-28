package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.result.IncidentPermissionResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

class IncidentPermissionResponseTest {

    @Test
    void fromResultMapsAllFields() {
        var now = LocalDateTime.now();
        var result = new IncidentPermissionResult("perm-1", "inc-1",
                LocalDate.now(), LocalDate.now().plusDays(1), 1, now, now);
        var response = IncidentPermissionResponse.from(result);

        assertEquals("perm-1", response.id());
        assertEquals("inc-1", response.incidenciaId());
        assertEquals(1, response.daysGranted());
        assertNotNull(response.startDate());
        assertNotNull(response.endDate());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }
}
