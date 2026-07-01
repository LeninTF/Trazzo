package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.result.IncidentResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDateTime;
import java.util.List;

class IncidentListResponseTest {

    @Test
    void fromPaginatedMapsAllFields() {
        var now = LocalDateTime.now();
        var results = List.of(
                new IncidentResult("i-1", "u-1", "t-1", IncidentState.PENDIENTE,
                        null, null, null, null, List.of(), null, now, now)
        );
        var paginated = new PaginatedResult<IncidentResult>(results, 0, 20, 1, 1);
        var response = IncidentListResponse.from(paginated, "MY_ASSIGNMENTS");

        assertEquals(1, response.content().size());
        assertEquals(0, response.page());
        assertEquals("MY_ASSIGNMENTS", response.scopeAplicado());
    }
}
