package trazzo.back.reports.application.ports.out;

import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for {@link MonthlyClosure} persistence.
 * <p>
 * Implementations MUST enforce a unique constraint on {@code (year, month)}
 * at the database level (e.g., a unique index or unique constraint).
 * Duplicate insert attempts SHOULD be translated into
 * {@link trazzo.back.reports.domain.exception.DuplicateClosureException}.
 */
public interface MonthlyClosureRepositoryPort {
    MonthlyClosure save(MonthlyClosure closure);
    Optional<MonthlyClosure> findById(UUID id);
    List<MonthlyClosure> findAll();
    List<MonthlyClosure> findByMonthAndYear(int month, int year);
}
