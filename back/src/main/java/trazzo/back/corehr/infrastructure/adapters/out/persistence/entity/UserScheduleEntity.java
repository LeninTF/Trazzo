package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "user_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserScheduleEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_user_id", nullable = false)
    private Long tenantUserId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "entry_time", nullable = false)
    private LocalTime entryTime;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;
}
