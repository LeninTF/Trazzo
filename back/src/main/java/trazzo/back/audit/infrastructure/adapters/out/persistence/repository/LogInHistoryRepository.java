package trazzo.back.audit.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trazzo.back.audit.domain.model.master.LogInHistory;

import java.util.List;

public interface LogInHistoryRepository extends JpaRepository<LogInHistory, Long> {
    List<LogInHistory> findTop10ByEmailOrderByTimestampDesc(String email);
}
