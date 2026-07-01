package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.TenantSettingsRecordEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TenantSettingsRecordMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = new TenantSettingsRecord(1L, "setting-1", "mydb", "localhost",
                "admin", "pass123", "user-1", "Schema update", now);

        var entity = TenantSettingsRecordMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals("setting-1", entity.getTenantSettingId());
        assertEquals("mydb", entity.getDbName());
        assertEquals("localhost", entity.getDbHost());
        assertEquals("admin", entity.getDbUser());
        assertEquals("pass123", entity.getDbPassword());
        assertEquals("user-1", entity.getUserId());
        assertEquals("Schema update", entity.getChangeReason());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new TenantSettingsRecordEntity();
        entity.setId(1L);
        entity.setTenantSettingId("setting-2");
        entity.setDbName("testdb");
        entity.setDbHost("db.example.com");
        entity.setDbUser("testuser");
        entity.setDbPassword("secret");
        entity.setUserId("user-2");
        entity.setChangeReason("Scaling up");
        entity.setCreatedAt(now);

        var domain = TenantSettingsRecordMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals("setting-2", domain.getTenantSettingId());
        assertEquals("testdb", domain.getDbName());
        assertEquals("db.example.com", domain.getDbHost());
        assertEquals("testuser", domain.getDbUser());
        assertEquals("secret", domain.getDbPassword());
        assertEquals("user-2", domain.getUserId());
        assertEquals("Scaling up", domain.getChangeReason());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = new TenantSettingsRecord(2L, "setting-3", "proddb", "prod.example.com",
                "produser", "prodpass", "user-3", "Production deploy", now);

        var entity = TenantSettingsRecordMapper.toEntity(original);
        var restored = TenantSettingsRecordMapper.toDomain(entity);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getTenantSettingId(), restored.getTenantSettingId());
        assertEquals(original.getDbName(), restored.getDbName());
        assertEquals(original.getDbHost(), restored.getDbHost());
        assertEquals(original.getDbUser(), restored.getDbUser());
        assertEquals(original.getDbPassword(), restored.getDbPassword());
        assertEquals(original.getUserId(), restored.getUserId());
        assertEquals(original.getChangeReason(), restored.getChangeReason());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void shouldThrowWhenChangeReasonIsNull() {
        var now = LocalDateTime.now();
        var entity = new TenantSettingsRecordEntity();
        entity.setId(3L);
        entity.setTenantSettingId("setting-4");
        entity.setDbName("nulldb");
        entity.setDbHost("nullhost");
        entity.setDbUser("nulluser");
        entity.setDbPassword("nullpass");
        entity.setUserId("user-4");
        entity.setChangeReason(null);
        entity.setCreatedAt(now);

        assertThrows(IllegalArgumentException.class,
                () -> TenantSettingsRecordMapper.toDomain(entity));
    }
}
