package trazzo.back.saasglobal.domain.model.multitenancy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.event.SaasGlobalDomainEvent;
import trazzo.back.saasglobal.domain.event.SubscriptionActivatedEvent;
import trazzo.back.saasglobal.domain.exception.InvalidSubscriptionTransitionException;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    private String id;
    private Integer planId;
    private String tenantId;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private SubscriptionStatus status;
    private BigDecimal purchasePrice;
    private String mpPreapprovalId;
    private LocalDateTime createdAt;
    private List<SaasGlobalDomainEvent> domainEvents = new ArrayList<>();
    Clock clock = Clock.systemDefaultZone();

    @SuppressWarnings("java:S107")
    private Subscription(
            String id,
            Integer planId,
            String tenantId,
            LocalDate dateStart,
            LocalDate dateEnd,
            SubscriptionStatus status,
            BigDecimal purchasePrice,
            String mpPreapprovalId,
            LocalDateTime createdAt
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.planId = requireNonNull(planId, "planId");
        this.tenantId = requireText(tenantId, "tenantId");
        this.dateStart = requireNonNull(dateStart, "dateStart");
        this.dateEnd = dateEnd;
        this.status = requireNonNull(status, "status");
        this.purchasePrice = requirePositive(purchasePrice);
        this.mpPreapprovalId = mpPreapprovalId;
        this.createdAt = createdAt;
    }

    public static Subscription createTrial(
            String tenantId,
            Integer planId,
            BigDecimal purchasePrice,
            LocalDate dateStart
    ) {
        return new Subscription(null, planId, tenantId, dateStart, null,
                SubscriptionStatus.TRIAL, purchasePrice, null, LocalDateTime.now(Clock.systemDefaultZone()));
    }

    @SuppressWarnings("java:S107")
    public static Subscription restore(
            String id,
            Integer planId,
            String tenantId,
            LocalDate dateStart,
            LocalDate dateEnd,
            SubscriptionStatus status,
            BigDecimal purchasePrice,
            String mpPreapprovalId,
            LocalDateTime createdAt
    ) {
        return new Subscription(id, planId, tenantId, dateStart, dateEnd, status, purchasePrice, mpPreapprovalId, createdAt);
    }

    public void activate(LocalDate dateEnd) {
        if (status != SubscriptionStatus.TRIAL) {
            throw new InvalidSubscriptionTransitionException(
                    "only TRIAL subscriptions can be activated, current status: " + status);
        }
        this.dateEnd = requireNonNull(dateEnd, "dateEnd");
        this.status = SubscriptionStatus.ACTIVE;
        recordEvent(new SubscriptionActivatedEvent(id, tenantId, planId, LocalDateTime.now(clock)));
    }

    public void linkMercadoPago(String mpPreapprovalId) {
        this.mpPreapprovalId = requireText(mpPreapprovalId, "mpPreapprovalId");
    }

    public void suspend() {
        if (status != SubscriptionStatus.ACTIVE) {
            throw new InvalidSubscriptionTransitionException(
                    "only ACTIVE subscriptions can be suspended, current status: " + status);
        }
        this.status = SubscriptionStatus.SUSPENDED;
    }

    public void cancel() {
        if (status == SubscriptionStatus.CANCELED) {
            throw new InvalidSubscriptionTransitionException("subscription is already canceled");
        }
        this.status = SubscriptionStatus.CANCELED;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isTrial() {
        return status == SubscriptionStatus.TRIAL;
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

    private static BigDecimal requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new TenantValidationException("purchasePrice must be zero or positive");
        }
        return value;
    }
}
