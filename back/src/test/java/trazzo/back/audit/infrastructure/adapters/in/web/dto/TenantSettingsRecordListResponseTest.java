package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSettingsRecordListResponseTest {

    @Test
    void shouldCreateFromPaginatedResult() {
        var now = LocalDateTime.now();
        var r1 = new TenantSettingsRecordResult(1L, "ts-1", "db1", "h1", "u1", "uid1", "reason1", now);
        var r2 = new TenantSettingsRecordResult(2L, "ts-2", "db2", "h2", "u2", "uid2", "reason2", now);
        var paginated = new PaginatedResult<TenantSettingsRecordResult>(List.of(r1, r2), 2, 5, 10, 2);

        var response = TenantSettingsRecordListResponse.from(paginated);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).tenantSettingId()).isEqualTo("ts-1");
        assertThat(response.content().get(1).dbName()).isEqualTo("db2");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(10);
        assertThat(response.totalPages()).isEqualTo(2);
    }
}
