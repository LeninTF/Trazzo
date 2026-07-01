package trazzo.back.audit.application.port.out;

import trazzo.back.audit.domain.model.tenant.SystemAudit;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemAuditRepositoryPort {
    List<SystemAudit> findAll(String searchTerm, String module, String entity,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
    long count(String searchTerm, String module, String entity,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta);
    Optional<SystemAudit> findById(Long id);
}
