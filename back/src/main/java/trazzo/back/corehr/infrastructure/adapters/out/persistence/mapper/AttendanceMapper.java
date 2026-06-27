package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;

public final class AttendanceMapper {

    private AttendanceMapper() {
    }

    public static AttendanceEntity toEntity(Attendance domain) {
        var entity = new AttendanceEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setScheduleId(domain.getScheduleId());
        entity.setDeviceId(domain.getDeviceId());
        entity.setCheckIn(domain.getCheckIn());
        entity.setCheckOut(domain.getCheckOut());
        entity.setAttendanceDate(domain.getAttendanceDate());
        entity.setMinutesLate(domain.getMinutesLate());
        entity.setState(domain.getState());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Attendance toDomain(AttendanceEntity entity) {
        return Attendance.restore(
                entity.getId(),
                entity.getTenantUserId(),
                entity.getScheduleId(),
                entity.getDeviceId(),
                entity.getCheckIn(),
                entity.getCheckOut(),
                entity.getAttendanceDate(),
                entity.getMinutesLate(),
                entity.getState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
