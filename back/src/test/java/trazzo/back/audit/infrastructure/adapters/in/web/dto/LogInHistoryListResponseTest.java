package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.domain.model.master.StatusLogin;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogInHistoryListResponseTest {

    @Test
    void shouldCreateFromPaginatedResult() {
        var now = LocalDateTime.now();
        var entry1 = new LogInHistoryResult("1", "u1", "a@b.com", StatusLogin.SUCCES,
                "ip1", "ua1", now);
        var entry2 = new LogInHistoryResult("2", "u2", "c@d.com", StatusLogin.FAILED_WRONG_PASSWORD,
                "ip2", "ua2", now);
        var paginated = new PaginatedResult<LogInHistoryResult>(List.of(entry1, entry2), 1, 10, 2, 1);

        var response = LogInHistoryListResponse.from(paginated);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).id()).isEqualTo("1");
        assertThat(response.content().get(1).attemptedEmail()).isEqualTo("c@d.com");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
