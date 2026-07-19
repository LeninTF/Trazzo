package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.domain.model.master.StatusLogin;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.LogInHistoryEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LogInHistoryMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = new LogInHistory("00000000-0000-0000-0000-000000000001", "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "test@example.com", StatusLogin.SUCCESS,
                "192.168.1.1", "Mozilla/5.0", now);

        var entity = LogInHistoryMapper.toEntity(domain);

        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", entity.getUserId().toString());
        assertEquals("test@example.com", entity.getAttemptedEmail());
        assertEquals(StatusLogin.SUCCESS, entity.getStatus());
        assertEquals("192.168.1.1", entity.getIpAddress());
        assertEquals("Mozilla/5.0", entity.getUserAgent());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new LogInHistoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
        entity.setAttemptedEmail("user@domain.com");
        entity.setStatus(StatusLogin.FAILED_WRONG_PASSWORD);
        entity.setIpAddress("10.0.0.1");
        entity.setUserAgent("curl/7.68");
        entity.setCreatedAt(now);

        var domain = LogInHistoryMapper.toDomain(entity);

        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901", domain.getUserId());
        assertEquals("user@domain.com", domain.getAttemptedEmail());
        assertEquals(StatusLogin.FAILED_WRONG_PASSWORD, domain.getStatus());
        assertEquals("10.0.0.1", domain.getIpAddress());
        assertEquals("curl/7.68", domain.getUserAgent());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = new LogInHistory("00000000-0000-0000-0000-000000000002", "c9d8e7f6-a5b4-3210-fedc-ba9876543210", "roundtrip@test.com",
                StatusLogin.LOCKED_OUT, "192.168.1.2", "PostmanRuntime/7.28", now);

        var entity = LogInHistoryMapper.toEntity(original);
        var restored = LogInHistoryMapper.toDomain(entity);

        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getAttemptedEmail(), restored.getAttemptedEmail());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getIpAddress(), restored.getIpAddress());
        assertEquals(original.getUserAgent(), restored.getUserAgent());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void shouldHandleNullUserId() {
        var now = LocalDateTime.now();
        var entity = new LogInHistoryEntity();
        entity.setId(UUID.randomUUID());
        entity.setAttemptedEmail("anon@test.com");
        entity.setStatus(StatusLogin.FAILED_USER_NOT_FOUND);
        entity.setIpAddress("0.0.0.0");
        entity.setUserAgent("unknown");
        entity.setCreatedAt(now);

        var domain = LogInHistoryMapper.toDomain(entity);

        assertNull(domain.getUserId());
        assertEquals("anon@test.com", domain.getAttemptedEmail());
        assertEquals(StatusLogin.FAILED_USER_NOT_FOUND, domain.getStatus());
    }

    @Test
    void shouldHandleNullIdInDomain_toEntity() {
        var domain = new LogInHistory(null, "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "test@test.com", StatusLogin.SUCCESS, "1.2.3.4", "agent", LocalDateTime.now());

        var entity = LogInHistoryMapper.toEntity(domain);

        assertNotNull(entity.getId());
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", entity.getUserId().toString());
    }

    @Test
    void shouldHandleNullIdInEntity_toDomain() {
        var entity = new LogInHistoryEntity();
        entity.setUserId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
        entity.setAttemptedEmail("test@test.com");
        entity.setStatus(StatusLogin.SUCCESS);
        entity.setIpAddress("1.2.3.4");
        entity.setUserAgent("agent");
        entity.setCreatedAt(LocalDateTime.now());

        var domain = LogInHistoryMapper.toDomain(entity);

        assertNull(domain.getId());
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", domain.getUserId());
    }

    @Test
    void shouldHandleNullUserIdInDomain_toEntity() {
        var domain = new LogInHistory("00000000-0000-0000-0000-000000000001", null,
                "anon@test.com", StatusLogin.FAILED_USER_NOT_FOUND, "1.2.3.4", "agent", LocalDateTime.now());

        var entity = LogInHistoryMapper.toEntity(domain);

        assertNull(entity.getUserId());
        assertEquals("anon@test.com", entity.getAttemptedEmail());
    }
}
