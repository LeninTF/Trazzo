package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SystemAuditResultTest {

    @Test
    void shouldCreate() {
        var now = LocalDateTime.now();
        var previousValue = Map.<String, Object>of("email", "old@test.com");
        var newValue = Map.<String, Object>of("email", "new@test.com");
        var result = new SystemAuditResult(
                1L, "tu-1", SystemActions.UPDATE, "users", "User",
                "42", HttpMethod.PUT, "/api/users/42",
                "Updated email", previousValue, newValue,
                "192.168.1.1", "SUCCESS", now
        );
        assertEquals(1L, result.id());
        assertEquals("tu-1", result.userTenantId());
        assertEquals(SystemActions.UPDATE, result.systemActions());
        assertEquals("users", result.module());
        assertEquals("User", result.entity());
        assertEquals("42", result.entityId());
        assertEquals(HttpMethod.PUT, result.httpMethod());
        assertEquals("/api/users/42", result.endpoint());
        assertEquals("Updated email", result.description());
        assertEquals(previousValue, result.previousValue());
        assertEquals(newValue, result.newValue());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("SUCCESS", result.result());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldHandleNullMaps() {
        var now = LocalDateTime.now();
        var result = new SystemAuditResult(
                1L, "tu-1", SystemActions.LOGIN, "auth", null,
                null, HttpMethod.GET, "/api/login", null,
                null, null, "10.0.0.1", "OK", now
        );
        assertNull(result.entity());
        assertNull(result.entityId());
        assertNull(result.description());
        assertNull(result.previousValue());
        assertNull(result.newValue());
    }

    @Test
    void shouldHandleDifferentActionsAndMethods() {
        var now = LocalDateTime.now();
        var result = new SystemAuditResult(
                1L, "tu-1", SystemActions.DELETE, "users", "User",
                "99", HttpMethod.DELETE, "/api/users/99",
                "Deleted user", null, null,
                "10.0.0.1", "SUCCESS", now
        );
        assertEquals(SystemActions.DELETE, result.systemActions());
        assertEquals(HttpMethod.DELETE, result.httpMethod());
    }

    @Test
    void shouldTestEquality() {
        var now = LocalDateTime.now();
        var r1 = new SystemAuditResult(1L, "tu", SystemActions.READ, "mod", "e", "1", HttpMethod.GET, "/e", "desc", null, null, "ip", "OK", now);
        var r2 = new SystemAuditResult(1L, "tu", SystemActions.READ, "mod", "e", "1", HttpMethod.GET, "/e", "desc", null, null, "ip", "OK", now);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new SystemAuditResult(null, null, SystemActions.CREATE, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(result.toString().contains("CREATE"));
    }
}
