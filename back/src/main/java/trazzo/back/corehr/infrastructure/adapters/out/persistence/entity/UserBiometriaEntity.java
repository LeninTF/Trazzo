package trazzo.back.corehr.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_biometria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBiometriaEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_user_id", nullable = false)
    private Long tenantUserId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "finger_index")
    private Integer fingerIndex;

    @Column(name = "template_cifrado", nullable = false, columnDefinition = "TEXT")
    private String templateCifrado;

    @Column(name = "llave_cifrado", length = 255)
    private String llaveCifrado;

    @Column(name = "capturado_en")
    private LocalDateTime capturadoEn;

    @Column(nullable = false)
    private boolean activo;
}
