package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.AuditLogResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogListResponseTest {

    @Test
    void shouldCreateFromPaginatedResult() {
        var now = LocalDateTime.now();
        var audit1 = new AuditLogResult("1", "evt-1", now, "t1", "t1", "u", "e",
                "CREATE", "T", "E", "1", "ip", "ua", null, null);
        var audit2 = new AuditLogResult("2", "evt-2", now, "t1", "t1", "u", "e",
                "UPDATE", "T", "E", "2", "ip", "ua", null, null);
        var paginated = new PaginatedResult<AuditLogResult>(List.of(audit1, audit2), 0, 20, 2, 1);

        var response = AuditLogListResponse.from(paginated);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo("1");
        assertThat(response.content().get(1).eventId()).isEqualTo("evt-2");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
