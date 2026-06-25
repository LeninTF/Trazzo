package trazzo.back.reports.application.ports.out;

import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyClosureRepositoryPort {
    MonthlyClosure save(MonthlyClosure closure);
    Optional<MonthlyClosure> findById(UUID id);
    List<MonthlyClosure> findAll();
    List<MonthlyClosure> findByMonthAndYear(int month, int year);
}
