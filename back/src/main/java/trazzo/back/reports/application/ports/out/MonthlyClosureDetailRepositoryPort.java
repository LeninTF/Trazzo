package trazzo.back.reports.application.ports.out;

import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyClosureDetailRepositoryPort {
    MonthlyClosureDetail save(MonthlyClosureDetail detail);
    List<MonthlyClosureDetail> saveAll(List<MonthlyClosureDetail> details);
    Optional<MonthlyClosureDetail> findById(UUID id);
    List<MonthlyClosureDetail> findByMonthlyClosureId(UUID monthlyClosureId);
}
