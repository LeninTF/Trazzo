package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSettingsRecordResponseTest {

    @Test
    void shouldCreateFromResult() {
        var now = LocalDateTime.now();
        var result = new TenantSettingsRecordResult(1L, "ts-1", "mydb", "dbhost:5432",
                "admin", "usr-1", "Schema update", now);

        var response = TenantSettingsRecordResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantSettingId()).isEqualTo("ts-1");
        assertThat(response.dbName()).isEqualTo("mydb");
        assertThat(response.dbHost()).isEqualTo("dbhost:5432");
        assertThat(response.dbUser()).isEqualTo("admin");
        assertThat(response.userId()).isEqualTo("usr-1");
        assertThat(response.changeReason()).isEqualTo("Schema update");
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
