package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.domain.model.tenant.SessionState;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionListResponseTest {

    @Test
    void shouldCreateFromPaginatedResult() {
        var now = LocalDateTime.now();
        var s1 = new SessionResult(1L, "tu1", "h1", "ip1", "ua1", "fp1",
                now, now, null, now.plusDays(1), SessionState.ACTIVE, now, now);
        var s2 = new SessionResult(2L, "tu2", "h2", "ip2", "ua2", "fp2",
                now, now, now, now, SessionState.LOGGED_OUT, now, now);
        var paginated = new PaginatedResult<SessionResult>(List.of(s1, s2), 0, 50, 2, 1);

        var response = SessionListResponse.from(paginated);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.content().get(1).tenantUserId()).isEqualTo("tu2");
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(50);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
