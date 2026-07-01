package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;

import java.time.LocalDateTime;
import java.util.List;

class IncidentTypeListResponseTest {

    @Test
    void fromPaginatedMapsAllFields() {
        var now = LocalDateTime.now();
        var results = List.of(
                new IncidentTypeResult("id-1", "Tipo 1", "Desc 1", true, now, now),
                new IncidentTypeResult("id-2", "Tipo 2", null, false, now, now)
        );
        var paginated = new PaginatedResult<IncidentTypeResult>(results, 0, 10, 2, 1);
        var response = IncidentTypeListResponse.from(paginated);

        assertEquals(2, response.content().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals("Tipo 1", response.content().getFirst().nombre());
    }

    @Test
    void fromPaginatedWithEmptyContent() {
        PaginatedResult<IncidentTypeResult> paginated = new PaginatedResult<>(List.of(), 0, 20, 0, 0);
        var response = IncidentTypeListResponse.from(paginated);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
    }
}
