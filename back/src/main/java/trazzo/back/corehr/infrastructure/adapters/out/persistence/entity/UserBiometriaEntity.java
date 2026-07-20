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

    @Column(name = "device_code", length = 100)
    private String deviceCode;

    @Column(name = "finger_index")
    private Integer fingerIndex;

    @Column(name = "encrypted_template_base64", nullable = false, columnDefinition = "TEXT")
    private String encryptedTemplateBase64;

    @Column(name = "encrypted_aes_key_base64", nullable = false, columnDefinition = "TEXT")
    private String encryptedAesKeyBase64;

    @Column(name = "iv_base64", columnDefinition = "TEXT")
    private String ivBase64;

    @Column(name = "tag_base64", columnDefinition = "TEXT")
    private String tagBase64;

    @Column(name = "capturado_en")
    private LocalDateTime capturadoEn;

    @Column(nullable = false)
    private boolean activo;
}
