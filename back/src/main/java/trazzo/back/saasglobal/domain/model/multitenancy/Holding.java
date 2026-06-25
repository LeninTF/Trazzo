package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    private Integer id;
    private String taxId;
    private String reasonSocial;
    private HoldingType type;
    private boolean state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    Clock clock = Clock.systemDefaultZone();

    @SuppressWarnings("java:S107")
    private Holding(
            Integer id,
            String taxId,
            String reasonSocial,
            HoldingType type,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.taxId = requireText(taxId, "taxId");
        this.reasonSocial = requireText(reasonSocial, "reasonSocial");
        this.type = requireNonNull(type, "type");
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Holding create(String taxId, String reasonSocial, HoldingType type) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Holding(null, taxId, reasonSocial, type, true, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static Holding restore(
            Integer id,
            String taxId,
            String reasonSocial,
            HoldingType type,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new Holding(id, taxId, reasonSocial, type, state, createdAt, updatedAt, deletedAt);
    }

    public void deactivate() {
        this.state = false;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void activate() {
        this.state = true;
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
