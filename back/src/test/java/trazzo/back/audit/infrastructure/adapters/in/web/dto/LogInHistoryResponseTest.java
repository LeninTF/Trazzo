package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.domain.model.master.StatusLogin;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LogInHistoryResponseTest {

    @Test
    void shouldCreateFromResult() {
        var now = LocalDateTime.now();
        var result = new LogInHistoryResult(1L, "usr-1", "test@test.com",
                StatusLogin.SUCCES, "192.168.1.1", "Chrome/120", now);

        var response = LogInHistoryResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo("usr-1");
        assertThat(response.attemptedEmail()).isEqualTo("test@test.com");
        assertThat(response.status()).isEqualTo(StatusLogin.SUCCES);
        assertThat(response.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(response.userAgent()).isEqualTo("Chrome/120");
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
