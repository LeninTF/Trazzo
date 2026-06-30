package trazzo.back.audit.application.port.out;

import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditRepositoryPort {
    List<Audit> findAll(String searchTerm, Action action, String entity,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
    long count(String searchTerm, Action action, String entity,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta);
    Optional<Audit> findById(Long id);
}
