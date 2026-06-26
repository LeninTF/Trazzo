package trazzo.back.saasglobal.domain.model.multitenancy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

    private Integer id;
    private String name;
    private BigDecimal price;
    private String currency;
    private String billingPeriod;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    Clock clock = Clock.systemDefaultZone();

    @SuppressWarnings("java:S107")
    private Plan(
            Integer id,
            String name,
            BigDecimal price,
            String currency,
            String billingPeriod,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.name = requireText(name, "name");
        this.price = requirePositive(price);
        this.currency = requireText(currency, "currency");
        this.billingPeriod = billingPeriod;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Plan create(String name, BigDecimal price, String currency, String billingPeriod) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Plan(null, name, price, currency, billingPeriod, true, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static Plan restore(
            Integer id,
            String name,
            BigDecimal price,
            String currency,
            String billingPeriod,
            boolean isActive,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new Plan(id, name, price, currency, billingPeriod, isActive, createdAt, updatedAt, deletedAt);
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("price must be zero or positive");
        }
        return value;
    }
}
