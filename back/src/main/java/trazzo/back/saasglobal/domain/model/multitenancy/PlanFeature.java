package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanFeature {

    private Integer id;
    private Integer planId;
    private Integer featureId;
    private String dataType;
    private String value;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @SuppressWarnings("java:S107")
    private PlanFeature(Integer id, Integer planId, Integer featureId, String dataType,
                        String value, LocalDate dateStart, LocalDate dateEnd, boolean active,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.planId = requireNonNull(planId, "planId");
        this.featureId = requireNonNull(featureId, "featureId");
        this.dataType = requireText(dataType, "dataType");
        this.value = requireText(value, "value");
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PlanFeature create(Integer planId, Integer featureId, String dataType,
                                     String value, LocalDate dateStart) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new PlanFeature(null, planId, featureId, dataType, value, dateStart, null, true, now, now);
    }

    @SuppressWarnings("java:S107")
    public static PlanFeature restore(Integer id, Integer planId, Integer featureId, String dataType,
                                      String value, LocalDate dateStart, LocalDate dateEnd, boolean active,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new PlanFeature(id, planId, featureId, dataType, value,
                dateStart, dateEnd, active, createdAt, updatedAt);
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }

    private static <T> T requireNonNull(T v, String fieldName) {
        if (v == null) throw new IllegalArgumentException(fieldName + " is required");
        return v;
    }
}
