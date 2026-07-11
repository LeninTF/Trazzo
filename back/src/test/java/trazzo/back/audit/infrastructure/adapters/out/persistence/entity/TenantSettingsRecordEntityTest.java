package trazzo.back.audit.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

class TenantSettingsRecordEntityTest {

    @Test
    void createInstanceWithAllArgsConstructor() {
        var now = LocalDateTime.now();
        var tenantId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        var entity = new TenantSettingsRecordEntity(
                1L, tenantId, "mydb", "localhost", "5432",
                "admin", "pass123", userId, "Schema update", now);

        assertEquals(1L, entity.getId());
        assertEquals(tenantId, entity.getTenantSettingId());
        assertEquals("mydb", entity.getDbName());
        assertEquals("localhost", entity.getDbHost());
        assertEquals("5432", entity.getDbPort());
        assertEquals("admin", entity.getDbUser());
        assertEquals("pass123", entity.getDbPassword());
        assertEquals(userId, entity.getUserId());
        assertEquals("Schema update", entity.getChangeReason());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new TenantSettingsRecordEntity();
        var tenantId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var now = LocalDateTime.now();

        entity.setId(2L);
        entity.setTenantSettingId(tenantId);
        entity.setDbName("testdb");
        entity.setDbHost("db.example.com");
        entity.setDbPort("3306");
        entity.setDbUser("testuser");
        entity.setDbPassword("secret");
        entity.setUserId(userId);
        entity.setChangeReason("Scaling up");
        entity.setCreatedAt(now);

        assertEquals(2L, entity.getId());
        assertEquals(tenantId, entity.getTenantSettingId());
        assertEquals("testdb", entity.getDbName());
        assertEquals("db.example.com", entity.getDbHost());
        assertEquals("3306", entity.getDbPort());
        assertEquals("testuser", entity.getDbUser());
        assertEquals("secret", entity.getDbPassword());
        assertEquals(userId, entity.getUserId());
        assertEquals("Scaling up", entity.getChangeReason());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void onCreate_shouldSetCreatedAt_whenNull() throws Exception {
        var entity = new TenantSettingsRecordEntity();
        assertNull(entity.getCreatedAt());

        var method = TenantSettingsRecordEntity.class.getDeclaredMethod("onCreate");
        method.setAccessible(true);
        method.invoke(entity);

        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void onCreate_shouldNotChangeCreatedAt_whenAlreadySet() throws Exception {
        var fixedTime = LocalDateTime.of(2025, 1, 15, 10, 30);
        var entity = new TenantSettingsRecordEntity();
        entity.setCreatedAt(fixedTime);

        var method = TenantSettingsRecordEntity.class.getDeclaredMethod("onCreate");
        method.setAccessible(true);
        method.invoke(entity);

        assertEquals(fixedTime, entity.getCreatedAt());
    }

    @Test
    void allFieldsCanBeSetToNull() {
        var entity = new TenantSettingsRecordEntity();
        entity.setId(null);
        entity.setTenantSettingId(null);
        entity.setDbName(null);
        entity.setDbHost(null);
        entity.setDbPort(null);
        entity.setDbUser(null);
        entity.setDbPassword(null);
        entity.setUserId(null);
        entity.setChangeReason(null);
        entity.setCreatedAt(null);

        assertNull(entity.getId());
        assertNull(entity.getTenantSettingId());
        assertNull(entity.getDbName());
        assertNull(entity.getDbHost());
        assertNull(entity.getDbPort());
        assertNull(entity.getDbUser());
        assertNull(entity.getDbPassword());
        assertNull(entity.getUserId());
        assertNull(entity.getChangeReason());
        assertNull(entity.getCreatedAt());
    }
}
