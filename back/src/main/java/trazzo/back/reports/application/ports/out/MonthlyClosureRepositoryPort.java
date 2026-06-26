package trazzo.back.reports.application.ports.out;

import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for {@link MonthlyClosure} persistence.
 */
public interface MonthlyClosureRepositoryPort {
    /**
     * Persists a monthly closure.
     * <p>
     * Implementations MUST enforce a unique constraint on {@code (year, month)}
     * at the database level (e.g., a unique index or unique constraint).
     * If a closure already exists for the same month and year, the implementation
     * MUST throw {@link trazzo.back.reports.domain.exception.DuplicateClosureException}.
     *
     * @param closure the closure to save
     * @return the saved closure
     * @throws trazzo.back.reports.domain.exception.DuplicateClosureException if a closure
     *         for the same month and year already exists
     */
    MonthlyClosure save(MonthlyClosure closure);
    Optional<MonthlyClosure> findById(UUID id);
    List<MonthlyClosure> findAll();
    List<MonthlyClosure> findByMonthAndYear(int month, int year);

    /**
     * Acquires a pessimistic lock for the given month/year and returns any existing closure.
     * <p>
     * Implementations MUST use {@code SELECT ... FOR UPDATE} (or equivalent) so that
     * the lock is held until the transaction commits. This prevents concurrent requests
     * from creating duplicate closures for the same period.
     *
     * @param month the month (1-12)
     * @param year  the year
     * @return the existing closure for the given period, if any
     */
    Optional<MonthlyClosure> findAndLockByMonthAndYear(int month, int year);
}
