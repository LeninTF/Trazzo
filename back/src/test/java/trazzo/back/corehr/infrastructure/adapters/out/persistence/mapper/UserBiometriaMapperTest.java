package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserBiometriaEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserBiometriaMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = UserBiometria.restore(1L, 10L, 5L, 1, "tmpl", "key",
                now, true, now, now);

        var entity = UserBiometriaMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTenantUserId());
        assertEquals(5L, entity.getDeviceId());
        assertEquals(1, entity.getFingerIndex());
        assertEquals("tmpl", entity.getTemplateCifrado());
        assertEquals("key", entity.getLlaveCifrado());
        assertTrue(entity.isActivo());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new UserBiometriaEntity();
        entity.setId(1L);
        entity.setTenantUserId(10L);
        entity.setDeviceId(5L);
        entity.setFingerIndex(2);
        entity.setTemplateCifrado("tmpl2");
        entity.setLlaveCifrado("key2");
        entity.setCapturadoEn(now);
        entity.setActivo(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = UserBiometriaMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(10L, domain.getTenantUserId());
        assertTrue(domain.isActivo());
    }

    @Test
    void shouldHandleNullOptionalFields() {
        var now = LocalDateTime.now();
        var domain = UserBiometria.restore(2L, 20L, null, null, "tmpl", "key",
                null, false, now, now);

        var entity = UserBiometriaMapper.toEntity(domain);
        var restored = UserBiometriaMapper.toDomain(entity);

        assertNull(restored.getDeviceId());
        assertNull(restored.getFingerIndex());
        assertEquals("key", restored.getLlaveCifrado());
        assertNull(restored.getCapturadoEn());
        assertFalse(restored.isActivo());
    }
}
