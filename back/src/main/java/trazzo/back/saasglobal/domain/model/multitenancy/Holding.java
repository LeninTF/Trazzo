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
    private String legalName;
    private HoldingType type;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @SuppressWarnings("java:S107")
    private Holding(Integer id, String taxId, String legalName, HoldingType type,
                    boolean active, LocalDateTime createdAt, LocalDateTime updatedAt,
                    LocalDateTime deletedAt) {
        this.id = id;
        this.taxId = requireText(taxId, "taxId");
        this.legalName = requireText(legalName, "legalName");
        this.type = requireNonNull(type, "type");
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Holding create(String taxId, String legalName, HoldingType type) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Holding(null, taxId, legalName, type, true, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static Holding restore(Integer id, String taxId, String legalName, HoldingType type,
                                  boolean active, LocalDateTime createdAt, LocalDateTime updatedAt,
                                  LocalDateTime deletedAt) {
        return new Holding(id, taxId, legalName, type, active, createdAt, updatedAt, deletedAt);
    }

    public void update(String legalName, HoldingType type) {
        this.legalName = requireText(legalName, "legalName");
        this.type = requireNonNull(type, "type");
        this.updatedAt = LocalDateTime.now(Clock.systemDefaultZone());
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now(Clock.systemDefaultZone());
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now(Clock.systemDefaultZone());
    }

    public void delete() {
        this.active = false;
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        this.updatedAt = now;
        this.deletedAt = now;
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
