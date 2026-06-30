package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAuditListResponseTest {

    @Test
    void shouldCreateFromPaginatedResult() {
        var now = LocalDateTime.now();
        var sa1 = new SystemAuditResult(1L, "tu1", SystemActions.CREATE, "mod", "E", "1",
                HttpMethod.POST, "/api/e", "created", null, null, "ip", "OK", now);
        var sa2 = new SystemAuditResult(2L, "tu2", SystemActions.DELETE, "mod", "E", "2",
                HttpMethod.DELETE, "/api/e/2", "deleted", null, null, "ip", "OK", now);
        var paginated = new PaginatedResult<SystemAuditResult>(List.of(sa1, sa2), 0, 10, 2, 1);

        var response = SystemAuditListResponse.from(paginated);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.content().get(1).endpoint()).isEqualTo("/api/e/2");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
