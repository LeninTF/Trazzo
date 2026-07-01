package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.employee.TenantContact;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantContactEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TenantContactMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = TenantContact.restore(1L, 10L, "EMAIL", now, now, null);

        var entity = TenantContactMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTenantUserId());
        assertEquals("EMAIL", entity.getType());
        assertNull(entity.getDeletedAt());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new TenantContactEntity();
        entity.setId(1L);
        entity.setTenantUserId(10L);
        entity.setType("PHONE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeletedAt(now);

        var domain = TenantContactMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals(10L, domain.getTenantUserId());
        assertEquals("PHONE", domain.getType());
        assertNotNull(domain.getDeletedAt());
    }

    @Test
    void shouldHandleDeletedAtNull() {
        var now = LocalDateTime.now();
        var domain = TenantContact.restore(2L, 20L, "EMAIL", now, now, null);

        var entity = TenantContactMapper.toEntity(domain);
        var restored = TenantContactMapper.toDomain(entity);

        assertNull(restored.getDeletedAt());
    }
}
