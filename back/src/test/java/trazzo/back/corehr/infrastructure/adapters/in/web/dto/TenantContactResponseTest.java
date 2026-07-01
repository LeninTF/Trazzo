package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.TenantContactResult;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class TenantContactResponseTest {

    @Test
    void fromMapsAllFieldsWithNestedUser() {
        var now = LocalDateTime.now();
        var user = new TenantContactResult.TenantUserBasicInfo(10L, "Juan", "Perez", "Garcia", "juan@test.com", "999-000");
        var result = new TenantContactResult(1L, 10L, "email", user, now, now, null);
        var response = TenantContactResponse.from(result);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantUserId()).isEqualTo(10L);
        assertThat(response.type()).isEqualTo("email");
        assertThat(response.tenantUser()).isNotNull();
        assertThat(response.tenantUser().nombre()).isEqualTo("Juan");
        assertThat(response.tenantUser().apellidoPaterno()).isEqualTo("Perez");
        assertThat(response.tenantUser().apellidoMaterno()).isEqualTo("Garcia");
        assertThat(response.tenantUser().email()).isEqualTo("juan@test.com");
        assertThat(response.tenantUser().phone()).isEqualTo("999-000");
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void fromHandlesNullUser() {
        var now = LocalDateTime.now();
        var result = new TenantContactResult(1L, 10L, "email", null, now, now, now);
        var response = TenantContactResponse.from(result);
        assertThat(response.tenantUser()).isNull();
        assertThat(response.deletedAt()).isNotNull();
    }

    @Test
    void innerRecord() {
        var r = new TenantContactResponse.TenantUserBasicInfoResponse(1L, "A", "B", "C", "e@m.com", "555");
        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.email()).isEqualTo("e@m.com");
        assertThat(r.phone()).isEqualTo("555");
    }

    @Test
    void equalsAndHashCode() {
        var now = LocalDateTime.now();
        var a = new TenantContactResponse(1L, 10L, "t", null, now, now, null);
        var b = new TenantContactResponse(1L, 10L, "t", null, now, now, null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
