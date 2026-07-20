package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserBiometriaEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserBiometriaMapperTest {

    private static final String TEMPLATE = "YmFzZTY0dGVtcGxhdGU=";
    private static final String AES_KEY = "YmFzZTY0YWVzS2V5";
    private static final String IV = "YmFzZTY0aXY=";
    private static final String TAG = "YmFzZTY0dGFn";

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = UserBiometria.restore(1L, 10L, 5L, "DVC-001", 1, TEMPLATE, AES_KEY, IV, TAG,
                now, true, now, now);

        var entity = UserBiometriaMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTenantUserId());
        assertEquals(5L, entity.getDeviceId());
        assertEquals(1, entity.getFingerIndex());
        assertEquals(TEMPLATE, entity.getEncryptedTemplateBase64());
        assertEquals(AES_KEY, entity.getEncryptedAesKeyBase64());
        assertTrue(entity.isActivo());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new UserBiometriaEntity();
        entity.setId(1L);
        entity.setTenantUserId(10L);
        entity.setDeviceId(5L);
        entity.setDeviceCode("DVC-001");
        entity.setFingerIndex(2);
        entity.setEncryptedTemplateBase64("dGVtcGxhdGUy");
        entity.setEncryptedAesKeyBase64("a2V5Mg==");
        entity.setIvBase64("aXYy");
        entity.setTagBase64("dGFnMg==");
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
        var domain = UserBiometria.restore(2L, 20L, null, null, null, TEMPLATE, AES_KEY, null, null,
                null, false, now, now);

        var entity = UserBiometriaMapper.toEntity(domain);
        var restored = UserBiometriaMapper.toDomain(entity);

        assertNull(restored.getDeviceId());
        assertNull(restored.getFingerIndex());
        assertEquals(AES_KEY, restored.getEncryptedAesKeyBase64());
        assertNull(restored.getCapturadoEn());
        assertFalse(restored.isActivo());
    }
}
