package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantBranding {

    private String tenantId;
    private String logoUrl;
    private String slogan;
    private String primaryColor;
    private String secondaryColor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    Clock clock = Clock.systemDefaultZone();

    private TenantBranding(
            String tenantId,
            String logoUrl,
            String slogan,
            String primaryColor,
            String secondaryColor,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.tenantId = tenantId; // nullable until associated with a persisted Tenant
        this.logoUrl = logoUrl;
        this.slogan = slogan;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @SuppressWarnings("java:S107")
    public static TenantBranding of(
            String tenantId,
            String logoUrl,
            String slogan,
            String primaryColor,
            String secondaryColor
    ) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new TenantBranding(tenantId, logoUrl, slogan, primaryColor, secondaryColor, now, now);
    }

    @SuppressWarnings("java:S107")
    public static TenantBranding restore(
            String tenantId,
            String logoUrl,
            String slogan,
            String primaryColor,
            String secondaryColor,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new TenantBranding(tenantId, logoUrl, slogan, primaryColor, secondaryColor, createdAt, updatedAt);
    }

    public void update(String logoUrl, String slogan, String primaryColor, String secondaryColor) {
        this.logoUrl = logoUrl;
        this.slogan = slogan;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.updatedAt = LocalDateTime.now(clock);
    }


}
