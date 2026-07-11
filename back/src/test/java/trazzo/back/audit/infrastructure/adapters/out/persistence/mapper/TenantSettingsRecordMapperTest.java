package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.TenantSettingsRecordEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantSettingsRecordMapperTest {

    private static final UUID TEST_TENANT_SETTING_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_USER_ID_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEST_TENANT_SETTING_ID_2 = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TEST_USER_ID_2 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TEST_TENANT_SETTING_ID_3 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TEST_USER_ID_3 = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID TEST_TENANT_SETTING_ID_4 = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID TEST_USER_ID_4 = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = new TenantSettingsRecord(1L, TEST_TENANT_SETTING_ID_1.toString(), "mydb", "localhost",
                "admin", "pass123", TEST_USER_ID_1.toString(), "Schema update", now);

        var entity = TenantSettingsRecordMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(TEST_TENANT_SETTING_ID_1, entity.getTenantSettingId());
        assertEquals("mydb", entity.getDbName());
        assertEquals("localhost", entity.getDbHost());
        assertEquals("admin", entity.getDbUser());
        assertEquals("pass123", entity.getDbPassword());
        assertEquals(TEST_USER_ID_1, entity.getUserId());
        assertEquals("Schema update", entity.getChangeReason());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new TenantSettingsRecordEntity();
        entity.setId(1L);
        entity.setTenantSettingId(TEST_TENANT_SETTING_ID_2);
        entity.setDbName("testdb");
        entity.setDbHost("db.example.com");
        entity.setDbUser("testuser");
        entity.setDbPassword("secret");
        entity.setUserId(TEST_USER_ID_2);
        entity.setChangeReason("Scaling up");
        entity.setCreatedAt(now);

        var domain = TenantSettingsRecordMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(TEST_TENANT_SETTING_ID_2.toString(), domain.getTenantSettingId());
        assertEquals("testdb", domain.getDbName());
        assertEquals("db.example.com", domain.getDbHost());
        assertEquals("testuser", domain.getDbUser());
        assertEquals("secret", domain.getDbPassword());
        assertEquals(TEST_USER_ID_2.toString(), domain.getUserId());
        assertEquals("Scaling up", domain.getChangeReason());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldMapRoundTrip() {
        var now = LocalDateTime.now();
        var original = new TenantSettingsRecord(2L, TEST_TENANT_SETTING_ID_3.toString(), "proddb", "prod.example.com",
                "produser", "prodpass", TEST_USER_ID_3.toString(), "Production deploy", now);

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
        entity.setTenantSettingId(TEST_TENANT_SETTING_ID_4);
        entity.setDbName("nulldb");
        entity.setDbHost("nullhost");
        entity.setDbUser("nulluser");
        entity.setDbPassword("nullpass");
        entity.setUserId(TEST_USER_ID_4);
        entity.setChangeReason(null);
        entity.setCreatedAt(now);

        assertThrows(IllegalArgumentException.class,
                () -> TenantSettingsRecordMapper.toDomain(entity));
    }

    @Test
    void shouldReturnNullForInvalidTenantSettingId() {
        var now = LocalDateTime.now();
        var domain = new TenantSettingsRecord(1L, "not-a-uuid", "db", "host",
                "user", "pass", TEST_USER_ID_1.toString(), "reason", now);

        var entity = TenantSettingsRecordMapper.toEntity(domain);

        assertNull(entity.getTenantSettingId());
        assertEquals(TEST_USER_ID_1, entity.getUserId());
    }

    @Test
    void shouldReturnNullForInvalidUserId() {
        var now = LocalDateTime.now();
        var domain = new TenantSettingsRecord(1L, TEST_TENANT_SETTING_ID_1.toString(), "db", "host",
                "user", "pass", "invalid-user-id", "reason", now);

        var entity = TenantSettingsRecordMapper.toEntity(domain);

        assertEquals(TEST_TENANT_SETTING_ID_1, entity.getTenantSettingId());
        assertNull(entity.getUserId());
    }

    @Test
    void shouldReturnNullForBlankTenantSettingId() {
        var now = LocalDateTime.now();
        var domain = new TenantSettingsRecord(1L, "  ", "db", "host",
                "user", "pass", TEST_USER_ID_1.toString(), "reason", now);

        var entity = TenantSettingsRecordMapper.toEntity(domain);

        assertNull(entity.getTenantSettingId());
    }

    @Test
    void shouldReturnNullForNullTenantSettingId() {
        var now = LocalDateTime.now();
        var domain = new TenantSettingsRecord(1L, null, "db", "host",
                "user", "pass", TEST_USER_ID_1.toString(), "reason", now);

        var entity = TenantSettingsRecordMapper.toEntity(domain);

        assertNull(entity.getTenantSettingId());
    }

    @Test
    void shouldMapToDomainWithNullTenantSettingId() {
        var now = LocalDateTime.now();
        var entity = new TenantSettingsRecordEntity();
        entity.setId(5L);
        entity.setTenantSettingId(null);
        entity.setDbName("db");
        entity.setDbHost("host");
        entity.setDbUser("user");
        entity.setDbPassword("pass");
        entity.setUserId(TEST_USER_ID_1);
        entity.setChangeReason("reason");
        entity.setCreatedAt(now);

        var domain = TenantSettingsRecordMapper.toDomain(entity);

        assertNull(domain.getTenantSettingId());
        assertEquals(TEST_USER_ID_1.toString(), domain.getUserId());
        assertEquals("db", domain.getDbName());
        assertEquals(now, domain.getCreatedAt());
    }

    @Test
    void shouldMapToDomainWithNullUserId() {
        var now = LocalDateTime.now();
        var entity = new TenantSettingsRecordEntity();
        entity.setId(6L);
        entity.setTenantSettingId(TEST_TENANT_SETTING_ID_1);
        entity.setDbName("db");
        entity.setDbHost("host");
        entity.setDbUser("user");
        entity.setDbPassword("pass");
        entity.setUserId(null);
        entity.setChangeReason("reason");
        entity.setCreatedAt(now);

        assertThrows(IllegalArgumentException.class,
                () -> TenantSettingsRecordMapper.toDomain(entity));
    }
}
