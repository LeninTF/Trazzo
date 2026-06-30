package trazzo.back.audit.domain.model.tenant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;

class AuditoriaSistemaTest {

    @Test
    void shouldCreateSystemAudit() {
        var now = LocalDateTime.now();
        var previous = Map.<String, Object>of("field", "old");
        var value = Map.<String, Object>of("field", "new");
        var s = new SystemAudit(1L, "tenant-user-1", SystemActions.UPDATE,
                "HR", "Employee", "42", HttpMethod.PUT,
                "/api/employees/42", "Updated employee details",
                previous, value, "10.0.0.1", "SUCCESS", now);
        assertEquals(1L, s.getId());
        assertEquals("tenant-user-1", s.getUserTenantId());
        assertEquals(SystemActions.UPDATE, s.getSystemActions());
        assertEquals("HR", s.getModule());
        assertEquals("Employee", s.getEntity());
        assertEquals("42", s.getEntityId());
        assertEquals(HttpMethod.PUT, s.getHttpMethod());
        assertEquals("/api/employees/42", s.getEndpoint());
        assertEquals("Updated employee details", s.getDescription());
        assertEquals(previous, s.getPreviousValue());
        assertEquals(value, s.getNewValue());
        assertEquals("10.0.0.1", s.getIpAddress());
        assertEquals("SUCCESS", s.getResult());
        assertEquals(now, s.getCreatedAt());
    }

}
