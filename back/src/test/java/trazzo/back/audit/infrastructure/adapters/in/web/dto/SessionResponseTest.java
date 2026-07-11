package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.domain.model.tenant.SessionState;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionResponseTest {

    @Test
    void shouldCreateFromResult() {
        var loginAt = LocalDateTime.now();
        var lastActivity = loginAt.plusHours(1);
        var logoutAt = loginAt.plusHours(8);
        var expiresAt = loginAt.plusDays(1);
        var createdAt = loginAt.minusMinutes(5);
        var updatedAt = loginAt;
        var result = new SessionResult(1L, "tuid-1", "hash-abc",
                "192.168.1.1", "Mozilla/5.0", "fp-001",
                loginAt, lastActivity, logoutAt, expiresAt,
                SessionState.ACTIVE, createdAt, updatedAt);

        var response = SessionResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantUserId()).isEqualTo("tuid-1");
        assertThat(response.refreshTokenHash()).isEqualTo("hash-abc");
        assertThat(response.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(response.userAgent()).isEqualTo("Mozilla/5.0");
        assertThat(response.deviceFingerprint()).isEqualTo("fp-001");
        assertThat(response.loginAt()).isEqualTo(loginAt);
        assertThat(response.lastActivityAt()).isEqualTo(lastActivity);
        assertThat(response.logoutAt()).isEqualTo(logoutAt);
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.state()).isEqualTo(SessionState.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}
