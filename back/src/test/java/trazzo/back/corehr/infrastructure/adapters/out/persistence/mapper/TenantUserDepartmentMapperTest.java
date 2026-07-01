package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantUserDepartmentEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TenantUserDepartmentMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = TenantUserDepartment.restore(1L, 10L, 20L, true, LocalDate.now(), null, now, now);

        var entity = TenantUserDepartmentMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals(10L, entity.getTenantUserId());
        assertEquals(20L, entity.getDepartmentId());
        assertTrue(entity.isPrimary());
        assertEquals(LocalDate.now(), entity.getStartDate());
        assertNull(entity.getEndDate());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var start = LocalDate.now();
        var entity = new TenantUserDepartmentEntity();
        entity.setId(1L);
        entity.setTenantUserId(10L);
        entity.setDepartmentId(20L);
        entity.setPrimary(true);
        entity.setStartDate(start);
        entity.setEndDate(start.plusDays(30));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = TenantUserDepartmentMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertTrue(domain.isPrimary());
        assertEquals(start.plusDays(30), domain.getEndDate());
    }

    @Test
    void shouldMapNonPrimary() {
        var now = LocalDateTime.now();
        var domain = TenantUserDepartment.restore(2L, 20L, 30L, false, LocalDate.now(), null, now, now);

        var entity = TenantUserDepartmentMapper.toEntity(domain);
        var restored = TenantUserDepartmentMapper.toDomain(entity);

        assertFalse(restored.isPrimary());
    }
}
