package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.command.CreateHoldingCommand;
import trazzo.back.saasglobal.application.dto.result.HoldingResult;
import trazzo.back.saasglobal.application.port.in.HoldingUseCase;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;

@Service
@RequiredArgsConstructor
public class HoldingService implements HoldingUseCase {

    private final HoldingRepositoryPort holdingRepository;

    @Override
    public HoldingResult create(CreateHoldingCommand command) {
        if (holdingRepository.existsByTaxId(command.taxId())) {
            throw new IllegalArgumentException("Tax ID already registered: " + command.taxId());
        }
        HoldingType type = HoldingType.valueOf(command.type());
        Holding holding = Holding.create(command.taxId(), command.legalName(), type);
        return toResult(holdingRepository.save(holding));
    }

    @Override
    public HoldingResult getById(Integer id) {
        return holdingRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found: " + id));
    }

    @Override
    public List<HoldingResult> listAll() {
        return holdingRepository.findAll().stream().map(this::toResult).toList();
    }

    @Override
    public HoldingResult activate(Integer id) {
        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found: " + id));
        holding.activate();
        return toResult(holdingRepository.save(holding));
    }

    @Override
    public HoldingResult deactivate(Integer id) {
        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found: " + id));
        holding.deactivate();
        return toResult(holdingRepository.save(holding));
    }

    private HoldingResult toResult(Holding h) {
        return new HoldingResult(
                h.getId(), h.getTaxId(), h.getLegalName(),
                h.getType().name(), h.isActive(),
                h.getCreatedAt(), h.getUpdatedAt());
    }
}
