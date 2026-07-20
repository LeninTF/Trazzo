package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.domain.model.master.Action;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogDetailResponseTest {

    @Test
    void shouldCreateFromResult() {
        var now = LocalDateTime.now();
        var previous = Map.<String, Object>of("name", "old");
        var updated = Map.<String, Object>of("name", "new");
        var result = new AuditLogDetailResult("1", "User", "usr-1", Action.CREATE,
                "uid-1", "/api/users", "10.0.0.1", "curl/7.0", previous, updated, now);

        var response = AuditLogDetailResponse.from(result);

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.entidad()).isEqualTo("User");
        assertThat(response.entidadId()).isEqualTo("usr-1");
        assertThat(response.accion()).isEqualTo(Action.CREATE);
        assertThat(response.userId()).isEqualTo("uid-1");
        assertThat(response.endpoint()).isEqualTo("/api/users");
        assertThat(response.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(response.userAgent()).isEqualTo("curl/7.0");
        assertThat(response.oldValue()).containsEntry("name", "old");
        assertThat(response.newValue()).containsEntry("name", "new");
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
