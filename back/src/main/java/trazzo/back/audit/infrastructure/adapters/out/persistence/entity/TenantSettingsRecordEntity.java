package trazzo.back.audit.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_settings_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettingsRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_setting_id")
    private UUID tenantSettingId;

    @Column(name = "db_name", length = 100)
    private String dbName;

    @Column(name = "db_host", length = 255)
    private String dbHost;

    @Column(name = "db_port", length = 10)
    private String dbPort;

    @Column(name = "db_user", length = 100)
    private String dbUser;

    @Column(name = "db_password", length = 255)
    private String dbPassword;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
