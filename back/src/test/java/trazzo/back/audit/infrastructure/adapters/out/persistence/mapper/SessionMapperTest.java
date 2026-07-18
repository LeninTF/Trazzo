package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SessionEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SessionMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = new Session(1L, "user-1", "hash123", "192.168.1.1",
                "Mozilla/5.0", "fp-001", now, now.plusHours(1), null,
                now.plusDays(7), SessionState.ACTIVE, now, now, now);

        var entity = SessionMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals("user-1", entity.getTenantUserId());
        assertEquals("hash123", entity.getRefreshTokenHash());
        assertEquals("192.168.1.1", entity.getIpAddress());
        assertEquals("Mozilla/5.0", entity.getUserAgent());
        assertEquals("fp-001", entity.getDeviceFingerprint());
        assertEquals(now, entity.getLoginAt());
        assertEquals(now.plusHours(1), entity.getLastActivityAt());
        assertNull(entity.getLogoutAt());
        assertEquals(now.plusDays(7), entity.getExpiresAt());
        assertEquals(SessionState.ACTIVE, entity.getState());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new SessionEntity();
        entity.setId(1L);
        entity.setTenantUserId("user-2");
        entity.setRefreshTokenHash("hash456");
        entity.setIpAddress("10.0.0.1");
        entity.setUserAgent("curl/7.68");
        entity.setDeviceFingerprint("fp-002");
        entity.setLoginAt(now);
        entity.setLastActivityAt(now.plusHours(2));
        entity.setLogoutAt(null);
        entity.setExpiresAt(now.plusDays(30));
        entity.setState(SessionState.ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = SessionMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals("user-2", domain.getTenantUserId());
        assertEquals("hash456", domain.getRefreshTokenHash());
        assertEquals("10.0.0.1", domain.getIpAddress());
        assertEquals("curl/7.68", domain.getUserAgent());
        assertEquals("fp-002", domain.getDeviceFingerprint());
        assertEquals(now, domain.getLoginAt());
        assertEquals(now.plusHours(2), domain.getLastActivityAt());
        assertNull(domain.getLogoutAt());
        assertEquals(now.plusDays(30), domain.getExpiresAt());
        assertEquals(SessionState.ACTIVE, domain.getState());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = new Session(2L, "user-3", "hash789", "192.168.1.3",
                "Edge/100", "fp-003", now, now.plusMinutes(30), null,
                now.plusDays(1), SessionState.ACTIVE, now, now, now);

        var entity = SessionMapper.toEntity(original);
        var restored = SessionMapper.toDomain(entity);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getTenantUserId(), restored.getTenantUserId());
        assertEquals(original.getRefreshTokenHash(), restored.getRefreshTokenHash());
        assertEquals(original.getIpAddress(), restored.getIpAddress());
        assertEquals(original.getUserAgent(), restored.getUserAgent());
        assertEquals(original.getDeviceFingerprint(), restored.getDeviceFingerprint());
        assertEquals(original.getLoginAt(), restored.getLoginAt());
        assertEquals(original.getLastActivityAt(), restored.getLastActivityAt());
        assertEquals(original.getLogoutAt(), restored.getLogoutAt());
        assertEquals(original.getExpiresAt(), restored.getExpiresAt());
        assertEquals(SessionState.ACTIVE, restored.getState());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
        assertEquals(original.getUpdatedAt(), restored.getUpdatedAt());
    }

    @Test
    void shouldMapInactiveState() {
        var now = LocalDateTime.now();
        var entity = new SessionEntity();
        entity.setId(3L);
        entity.setTenantUserId("user-4");
        entity.setRefreshTokenHash("hash-loggedout");
        entity.setIpAddress("10.0.0.4");
        entity.setUserAgent("Safari/15");
        entity.setDeviceFingerprint("fp-004");
        entity.setLoginAt(now.minusDays(1));
        entity.setLastActivityAt(now.minusHours(1));
        entity.setLogoutAt(now);
        entity.setExpiresAt(now.minusDays(1).plusDays(7));
        entity.setState(SessionState.LOGGED_OUT);
        entity.setCreatedAt(now.minusDays(1));
        entity.setUpdatedAt(now);

        var domain = SessionMapper.toDomain(entity);

        assertEquals(SessionState.LOGGED_OUT, domain.getState());
    }
}
