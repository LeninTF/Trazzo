package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.DeviceEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DeviceMapperTest {

    @Test
    void shouldMapToEntity() {
        var now = LocalDateTime.now();
        var domain = Device.restore(1L, "DEV-001", "Reader1", "192.168.1.1", 8080,
                "Office", 10L, true, now);

        var entity = DeviceMapper.toEntity(domain);

        assertEquals(1L, entity.getId());
        assertEquals("DEV-001", entity.getCode());
        assertEquals("Reader1", entity.getName());
        assertEquals("192.168.1.1", entity.getIp());
        assertEquals(8080, entity.getPuerto());
        assertEquals("Office", entity.getUbicacion());
        assertEquals(10L, entity.getBranchId());
        assertTrue(entity.isState());
    }

    @Test
    void shouldMapToDomain() {
        var now = LocalDateTime.now();
        var entity = new DeviceEntity();
        entity.setId(1L);
        entity.setCode("DEV-002");
        entity.setName("Reader2");
        entity.setIp("10.0.0.1");
        entity.setPuerto(9090);
        entity.setUbicacion("Entrance");
        entity.setBranchId(20L);
        entity.setState(false);
        entity.setCreatedAt(now);

        var domain = DeviceMapper.toDomain(entity);

        assertEquals(1L, domain.getId());
        assertEquals("DEV-002", domain.getCode());
        assertFalse(domain.isState());
    }

    @Test
    void shouldHandleNullOptionalFields() {
        var now = LocalDateTime.now();
        var domain = Device.restore(2L, "DEV-003", null, null, null, null, null, true, now);

        var entity = DeviceMapper.toEntity(domain);
        var restored = DeviceMapper.toDomain(entity);

        assertNull(restored.getName());
        assertNull(restored.getIp());
        assertNull(restored.getPuerto());
        assertNull(restored.getUbicacion());
        assertNull(restored.getBranchId());
    }
}
