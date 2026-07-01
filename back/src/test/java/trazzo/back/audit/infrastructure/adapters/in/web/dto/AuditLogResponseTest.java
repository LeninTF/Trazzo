package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.AuditLogResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogResponseTest {

    @Test
    void shouldCreateFromResult() {
        var now = LocalDateTime.now();
        var oldValue = Map.<String, Object>of("field1", "old");
        var newValue = Map.<String, Object>of("field1", "new");
        var result = new AuditLogResult("1", "evt-1", now, "tenant1", "tnt-1",
                "jdoe", "jdoe@test.com", "UPDATE", "USER", "User", "usr-1",
                "192.168.1.1", "Mozilla/5.0", oldValue, newValue);

        var response = AuditLogResponse.from(result);

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.eventId()).isEqualTo("evt-1");
        assertThat(response.fecha()).isEqualTo(now);
        assertThat(response.tenant()).isEqualTo("tenant1");
        assertThat(response.tenantId()).isEqualTo("tnt-1");
        assertThat(response.userName()).isEqualTo("jdoe");
        assertThat(response.userEmail()).isEqualTo("jdoe@test.com");
        assertThat(response.accion()).isEqualTo("UPDATE");
        assertThat(response.tipo()).isEqualTo("USER");
        assertThat(response.entidad()).isEqualTo("User");
        assertThat(response.entidadId()).isEqualTo("usr-1");
        assertThat(response.ipAddress()).isEqualTo("192.168.1.1");
        assertThat(response.userAgent()).isEqualTo("Mozilla/5.0");
        assertThat(response.oldValue()).containsEntry("field1", "old");
        assertThat(response.newValue()).containsEntry("field1", "new");
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var result = new AuditLogResult("1", "evt-1", now, "t1", "t1", "u", "e",
                "CREATE", "T", "E", "1", "ip", "ua", null, null);
        var a = AuditLogResponse.from(result);
        var b = AuditLogResponse.from(result);

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
