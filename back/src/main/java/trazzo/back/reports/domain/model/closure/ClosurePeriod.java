package trazzo.back.reports.domain.model.closure;

import trazzo.back.reports.domain.exception.InvalidClosurePeriodException;

public record ClosurePeriod(int month, int year) {

    public ClosurePeriod {
        if (month < 1 || month > 12)
            throw new InvalidClosurePeriodException("Month must be between 1 and 12");
        if (year < 2000)
            throw new InvalidClosurePeriodException("Year must be >= 2000");
    }
}
