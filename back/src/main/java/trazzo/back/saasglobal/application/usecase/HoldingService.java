package trazzo.back.saasglobal.application.usecase;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.dto.command.CreateHoldingCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateHoldingCommand;
import trazzo.back.saasglobal.application.dto.result.HoldingResult;
import trazzo.back.saasglobal.application.port.in.HoldingUseCase;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;

@Service
@RequiredArgsConstructor
@Transactional
public class HoldingService implements HoldingUseCase {

    private static final String NOT_FOUND_MSG = "Holding not found: ";

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
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
    }

    @Override
    public List<HoldingResult> listAll() {
        return holdingRepository.findAll().stream().map(this::toResult).toList();
    }

    @Override
    public HoldingResult update(UpdateHoldingCommand command) {
        Holding holding = holdingRepository.findById(command.id())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + command.id()));
        holding.update(command.legalName(), HoldingType.valueOf(command.type()));
        return toResult(holdingRepository.save(holding));
    }

    @Override
    public HoldingResult activate(Integer id) {
        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        holding.activate();
        return toResult(holdingRepository.save(holding));
    }

    @Override
    public HoldingResult deactivate(Integer id) {
        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        holding.deactivate();
        return toResult(holdingRepository.save(holding));
    }

    @Override
    public void deleteById(Integer id) {
        Holding holding = holdingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND_MSG + id));
        holding.delete();
        holdingRepository.save(holding);
    }

    private HoldingResult toResult(Holding h) {
        return new HoldingResult(
                h.getId(), h.getTaxId(), h.getLegalName(),
                h.getType().name(), h.isActive(),
                h.getCreatedAt(), h.getUpdatedAt());
    }
}
