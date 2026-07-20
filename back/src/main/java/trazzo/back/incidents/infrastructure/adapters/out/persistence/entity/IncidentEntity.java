package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_user_id", nullable = false, length = 36)
    private String tenantUserId;

    @Column(name = "incidencia_type_id", nullable = false, length = 36)
    private String incidentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentState state;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "incidencia_id")
    private List<IncidentEvidenceEntity> evidences = new ArrayList<>();

    @Transient
    private IncidentPermissionEntity permission;

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
