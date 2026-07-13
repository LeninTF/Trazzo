package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.event.SaasGlobalDomainEvent;
import trazzo.back.saasglobal.domain.event.TenantActivatedEvent;
import trazzo.back.saasglobal.domain.exception.InvalidTenantTransitionException;
import trazzo.back.saasglobal.domain.exception.TenantAlreadyActivatedException;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    private String id;
    private Integer holdingId;
    private String subDomain;
    private Integer planId;
    private TenantSettings settings;
    private TenantBranding branding;
    private LocalDateTime activatedAt;
    private LocalDateTime suspendedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private List<SaasGlobalDomainEvent> domainEvents = new ArrayList<>();
    Clock clock = Clock.systemDefaultZone();

    @SuppressWarnings("java:S107")
    private Tenant(
            String id,
            Integer holdingId,
            String subDomain,
            Integer planId,
            TenantSettings settings,
            TenantBranding branding,
            LocalDateTime activatedAt,
            LocalDateTime suspendedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.holdingId = holdingId;
        this.subDomain = requireText(subDomain, "subDomain");
        this.planId = requireNonNull(planId, "planId");
        this.settings = settings;
        this.branding = branding;
        this.activatedAt = activatedAt;
        this.suspendedAt = suspendedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * TRIAL manual: admin provides DB settings upfront.
     */
    public static Tenant createTrial(
            String subDomain,
            Integer planId,
            Integer holdingId,
            TenantSettings settings,
            TenantBranding branding
    ) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        TenantSettings settingsWithId = requireNonNull(settings, "settings");
        return new Tenant(id, holdingId, subDomain, planId, settingsWithId, branding, null, null, now, now, null);
    }

    /**
     * PAID flow: tenant created without settings, assigned after payment confirmation.
     */
    public static Tenant createPending(String subDomain, Integer planId, Integer holdingId) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Tenant(null, holdingId, subDomain, planId, null, null, null, null, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static Tenant restore(
            String id,
            Integer holdingId,
            String subDomain,
            Integer planId,
            TenantSettings settings,
            TenantBranding branding,
            LocalDateTime activatedAt,
            LocalDateTime suspendedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new Tenant(id, holdingId, subDomain, planId, settings, branding, activatedAt, suspendedAt, createdAt, updatedAt, deletedAt);
    }

    public void assignSettings(TenantSettings settings) {
        if (isActivated()) {
            throw new TenantAlreadyActivatedException("cannot change settings of an already activated tenant");
        }
        this.settings = requireNonNull(settings, "settings");
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void assignBranding(TenantBranding branding) {
        this.branding = branding;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void activate() {
        if (isActivated()) {
            throw new TenantAlreadyActivatedException("tenant is already activated");
        }
        if (settings == null) {
            throw new TenantValidationException("settings must be assigned before activating tenant");
        }
        this.activatedAt = LocalDateTime.now(clock);
        this.updatedAt = this.activatedAt;
        recordEvent(new TenantActivatedEvent(id, subDomain, activatedAt));
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now(clock);
        this.updatedAt = this.deletedAt;
    }

    public void suspend() {
        if (!isActivated()) {
            throw new InvalidTenantTransitionException("cannot suspend a tenant that is not activated");
        }
        if (isSuspended()) {
            throw new InvalidTenantTransitionException("tenant is already suspended");
        }
        this.suspendedAt = LocalDateTime.now(clock);
        this.updatedAt = this.suspendedAt;
    }

    public void reactivate() {
        if (!isSuspended()) {
            throw new InvalidTenantTransitionException("cannot reactivate a tenant that is not suspended");
        }
        this.suspendedAt = null;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public boolean isActivated() {
        return activatedAt != null;
    }

    public boolean isSuspended() {
        return suspendedAt != null;
    }

    public boolean hasSettings() {
        return settings != null;
    }

    public List<SaasGlobalDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public List<SaasGlobalDomainEvent> pullDomainEvents() {
        List<SaasGlobalDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void recordEvent(SaasGlobalDomainEvent event) {
        this.domainEvents.add(event);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new TenantValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new TenantValidationException(fieldName + " is required");
        }
        return value;
    }
}
