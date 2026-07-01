package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAuditResponseTest {

    @Test
    void shouldCreateFromResult() {
        var now = LocalDateTime.now();
        var previous = Map.<String, Object>of("key", "old");
        var updated = Map.<String, Object>of("key", "new");
        var result = new SystemAuditResult(1L, "tuid-1", SystemActions.LOGIN,
                "auth", "User", "usr-1", HttpMethod.POST, "/api/login",
                "User logged in", previous, updated, "10.0.0.1", "SUCCESS", now);

        var response = SystemAuditResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userTenantId()).isEqualTo("tuid-1");
        assertThat(response.systemActions()).isEqualTo(SystemActions.LOGIN);
        assertThat(response.module()).isEqualTo("auth");
        assertThat(response.entity()).isEqualTo("User");
        assertThat(response.entityId()).isEqualTo("usr-1");
        assertThat(response.httpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(response.endpoint()).isEqualTo("/api/login");
        assertThat(response.description()).isEqualTo("User logged in");
        assertThat(response.previousValue()).containsEntry("key", "old");
        assertThat(response.newValue()).containsEntry("key", "new");
        assertThat(response.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(response.result()).isEqualTo("SUCCESS");
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
