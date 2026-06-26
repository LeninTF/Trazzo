package trazzo.back.incidents.domain.specification;

import java.time.LocalDate;

public class IncidentPermissionSpec {

    public boolean hasValidPeriod(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public boolean hasValidDaysGranted(int daysGranted) {
        return daysGranted > 0;
    }
}
