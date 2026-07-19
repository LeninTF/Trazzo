package trazzo.back.saasglobal.application.port.out;

import java.time.LocalDateTime;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.request.RequestContact;

public interface RequestContactRepositoryPort {
    RequestContact save(RequestContact contact);
    Optional<RequestContact> findByRequestId(Integer requestId);
    long countByTaxId(String taxId);
    boolean existsRecentByTaxId(String taxId, LocalDateTime since);
}
