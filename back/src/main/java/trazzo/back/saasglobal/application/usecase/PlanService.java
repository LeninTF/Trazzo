package trazzo.back.saasglobal.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.application.port.out.FeatureRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanFeatureRepositoryPort;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;
import trazzo.back.saasglobal.domain.model.multitenancy.PlanFeature;

@Service
@RequiredArgsConstructor
public class PlanService implements PlanUseCase {

    private static final String NOT_FOUND_MSG = "Plan not found: ";

    private final PlanRepositoryPort planRepository;
    private final PlanFeatureRepositoryPort planFeatureRepository;
    private final FeatureRepositoryPort featureRepository;
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    public PlanResult create(CreatePlanCommand command) {
        Plan plan = Plan.create(command.name(), command.price(), command.priceAnnual(),
                command.currency(), command.billingPeriod());
        Plan saved = planRepository.save(plan);
        replaceFeatures(saved.getId(), command.features());
        return toResult(saved);
    }

    @Override
    public PlanResult getById(Integer id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        return toResult(plan);
    }

    @Override
    public List<PlanResult> listActive() {
        return planRepository.findAllActive().stream().map(this::toResult).toList();
    }

    @Override
    public List<PlanResult> listAll() {
        return planRepository.findAll().stream().map(this::toResult).toList();
    }

    @Override
    public PlanResult update(UpdatePlanCommand command) {
        Plan plan = planRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + command.id()));
        plan.update(command.name(), command.price(), command.priceAnnual(),
                command.currency(), command.billingPeriod());
        Plan saved = planRepository.save(plan);
        replaceFeatures(saved.getId(), command.features());
        return toResult(saved);
    }

    @Override
    public PlanResult activate(Integer id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        plan.activate();
        return toResult(planRepository.save(plan));
    }

    @Override
    public PlanResult deactivate(Integer id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        plan.deactivate();
        return toResult(planRepository.save(plan));
    }

    @Override
    public void deleteById(Integer id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        plan.delete();
        planRepository.save(plan);
    }

    /**
     * Plan features are always submitted as a complete set from the admin UI, so each save
     * replaces the plan's whole feature set rather than diffing individual entries. Boolean
     * features are only stored when true (absence = disabled); numeric features are always
     * stored since they represent core plan limits.
     */
    private void replaceFeatures(Integer planId, Map<String, Object> features) {
        planFeatureRepository.deleteByPlanId(planId);
        if (features == null || features.isEmpty()) {
            return;
        }
        Map<String, Integer> featureIdsByName = featureRepository.findAll().stream()
                .collect(Collectors.toMap(Feature::getName, Feature::getId));
        LocalDate today = LocalDate.now(clock);

        features.forEach((name, rawValue) -> {
            Integer featureId = featureIdsByName.get(name);
            if (featureId == null || rawValue == null) {
                return;
            }
            if (rawValue instanceof Boolean bool) {
                if (bool) {
                    planFeatureRepository.save(
                            PlanFeature.create(planId, featureId, "BOOLEAN", "true", today));
                }
            } else if (rawValue instanceof Number number) {
                planFeatureRepository.save(
                        PlanFeature.create(planId, featureId, "INT", String.valueOf(number.longValue()), today));
            }
        });
    }

    private PlanResult toResult(Plan p) {
        return new PlanResult(
                p.getId(), p.getName(), p.getPrice(), p.getPriceAnnual(), p.getCurrency(),
                p.getBillingPeriod(), p.isActive(), p.getCreatedAt(), loadFeatures(p.getId()));
    }

    private Map<String, Object> loadFeatures(Integer planId) {
        if (planId == null) {
            return Map.of();
        }
        Map<Integer, String> featureNamesById = featureRepository.findAll().stream()
                .collect(Collectors.toMap(Feature::getId, Feature::getName));
        return planFeatureRepository.findByPlanId(planId).stream()
                .filter(pf -> featureNamesById.containsKey(pf.getFeatureId()))
                .collect(Collectors.toMap(
                        pf -> featureNamesById.get(pf.getFeatureId()),
                        this::parseFeatureValue));
    }

    private Object parseFeatureValue(PlanFeature pf) {
        if ("BOOLEAN".equals(pf.getDataType())) {
            return Boolean.parseBoolean(pf.getValue());
        }
        if ("INT".equals(pf.getDataType()) || "LONG".equals(pf.getDataType())) {
            return Long.parseLong(pf.getValue());
        }
        return pf.getValue();
    }
}
