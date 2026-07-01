package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreatePlanCommand;
import trazzo.back.saasglobal.application.dto.command.UpdatePlanCommand;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

@Service
@RequiredArgsConstructor
public class PlanService implements PlanUseCase {

    private static final String NOT_FOUND_MSG = "Plan not found: ";

    private final PlanRepositoryPort planRepository;

    @Override
    public PlanResult create(CreatePlanCommand command) {
        Plan plan = Plan.create(command.name(), command.price(), command.currency(), command.billingPeriod());
        return toResult(planRepository.save(plan));
    }

    @Override
    public PlanResult getById(Integer id) {
        return planRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
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
        plan.update(command.name(), command.price(), command.currency(), command.billingPeriod());
        return toResult(planRepository.save(plan));
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

    private PlanResult toResult(Plan p) {
        return new PlanResult(
                p.getId(), p.getName(), p.getPrice(), p.getCurrency(),
                p.getBillingPeriod(), p.isActive(), p.getCreatedAt());
    }
}
