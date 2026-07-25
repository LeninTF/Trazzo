package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEntity extends AuditableEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_user_id", nullable = false)
    private Long tenantUserId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "minutes_late", nullable = false)
    private int minutesLate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceState state;

    @Column(name = "offline_event_id")
    private Integer offlineEventId;

    @Column(name = "device_code", length = 100)
    private String deviceCode;
}
