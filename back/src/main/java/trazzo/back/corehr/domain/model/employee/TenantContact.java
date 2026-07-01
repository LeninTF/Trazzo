package trazzo.back.corehr.domain.model.employee;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.model.BaseDomainModel;
import trazzo.back.corehr.domain.model.DomainModelValidator;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantContact extends BaseDomainModel {

    private Long tenantUserId;
    private String type;
    private LocalDateTime deletedAt;

    private TenantContact(
            Long id,
            Long tenantUserId,
            String type,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        super(id, createdAt, updatedAt);
        this.tenantUserId = DomainModelValidator.requireTenantUserId(tenantUserId);
        this.type = DomainModelValidator.requireText(type, "type");
        this.deletedAt = deletedAt;
    }

    public static TenantContact create(Long tenantUserId, String type) {
        LocalDateTime now = LocalDateTime.now();
        return new TenantContact(null, tenantUserId, type, now, now, null);
    }

    public static TenantContact restore(
            Long id,
            Long tenantUserId,
            String type,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new TenantContact(id, tenantUserId, type, createdAt, updatedAt, deletedAt);
    }

    public void updateType(String type) {
        this.type = DomainModelValidator.requireText(type, "type");
        touch();
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now(clock);
        touch();
    }
}
