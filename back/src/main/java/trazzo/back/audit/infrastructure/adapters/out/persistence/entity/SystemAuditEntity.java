package trazzo.back.audit.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_user_id")
    private Long tenantUserId;

    @Column(name = "sesion_id")
    private Long sesionId;

    @Column(name = "accion_sistema", nullable = false, length = 50)
    private String accionSistema;

    @Column(nullable = false, length = 100)
    private String modulo;

    @Column(name = "entidad_id", length = 100)
    private String entidadId;

    @Column(name = "valores_prev", columnDefinition = "jsonb")
    private String valoresPrev;

    @Column(length = 255)
    private String endpoint;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "valor_anterior", columnDefinition = "jsonb")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "jsonb")
    private String valorNuevo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String resultado;

    @Column(name = "date")
    private LocalDateTime date;

    @PrePersist
    protected void onCreate() {
        if (date == null) {
            date = LocalDateTime.now();
        }
    }
}
