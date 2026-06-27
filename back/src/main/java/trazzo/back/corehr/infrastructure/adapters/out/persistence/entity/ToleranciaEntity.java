package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trazzo.back.corehr.domain.model.ToleranciaType;

import java.time.LocalDateTime;

@Entity
@Table(name = "tolerancia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ToleranciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToleranciaType type;

    @Column(nullable = false)
    private Integer minutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
