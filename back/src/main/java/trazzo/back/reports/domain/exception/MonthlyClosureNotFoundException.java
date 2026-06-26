package trazzo.back.reports.domain.exception;

import java.util.UUID;

public class MonthlyClosureNotFoundException extends RuntimeException {
    public MonthlyClosureNotFoundException(UUID id) {
        super("Monthly closure not found with id: " + id);
    }
}
