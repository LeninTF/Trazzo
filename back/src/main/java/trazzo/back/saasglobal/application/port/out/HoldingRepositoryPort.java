package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;

public interface HoldingRepositoryPort {
    Holding save(Holding holding);
    Optional<Holding> findById(Integer id);
    Optional<Holding> findByTaxId(String taxId);
    List<Holding> findAll();
    boolean existsByTaxId(String taxId);
}
