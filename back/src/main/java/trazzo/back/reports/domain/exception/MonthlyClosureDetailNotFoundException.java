package trazzo.back.reports.domain.exception;

import java.util.UUID;

public class MonthlyClosureDetailNotFoundException extends RuntimeException {
    public MonthlyClosureDetailNotFoundException(UUID id) {
        super("Monthly closure detail not found with id: " + id);
    }
}
