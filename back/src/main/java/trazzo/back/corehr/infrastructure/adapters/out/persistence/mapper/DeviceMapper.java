package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.DeviceEntity;

public final class DeviceMapper {

    private DeviceMapper() {
    }

    public static DeviceEntity toEntity(Device domain) {
        var entity = new DeviceEntity();
        entity.setId(domain.getId());
        entity.setCode(domain.getCode());
        entity.setName(domain.getName());
        entity.setIp(domain.getIp());
        entity.setPuerto(domain.getPuerto());
        entity.setUbicacion(domain.getUbicacion());
        entity.setBranchId(domain.getBranchId());
        entity.setState(domain.isState());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static Device toDomain(DeviceEntity entity) {
        return Device.restore(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getIp(),
                entity.getPuerto(),
                entity.getUbicacion(),
                entity.getBranchId(),
                entity.isState(),
                entity.getCreatedAt()
        );
    }
}
