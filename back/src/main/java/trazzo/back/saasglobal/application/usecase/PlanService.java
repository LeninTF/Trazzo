package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.result.PlanResult;
import trazzo.back.saasglobal.application.port.in.PlanUseCase;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

@Service
@RequiredArgsConstructor
public class PlanService implements PlanUseCase {

    private final PlanRepositoryPort planRepository;

    @Override
    public PlanResult getById(Integer id) {
        return planRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + id));
    }

    @Override
    public List<PlanResult> listActive() {
        return planRepository.findAllActive().stream().map(this::toResult).toList();
    }

    @Override
    public List<PlanResult> listAll() {
        return planRepository.findAll().stream().map(this::toResult).toList();
    }

    private PlanResult toResult(Plan p) {
        return new PlanResult(
                p.getId(), p.getName(), p.getPrice(), p.getCurrency(),
                p.getBillingPeriod(), p.isActive(), p.getCreatedAt());
    }
}
