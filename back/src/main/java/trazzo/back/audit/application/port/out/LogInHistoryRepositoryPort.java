package trazzo.back.audit.application.port.out;

import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.domain.model.master.StatusLogin;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LogInHistoryRepositoryPort {
    List<LogInHistory> findAll(String userId, String attemptedEmail, StatusLogin status,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
    long count(String userId, String attemptedEmail, StatusLogin status,
        LocalDateTime fechaDesde, LocalDateTime fechaHasta);
    Optional<LogInHistory> findById(String id);
}
