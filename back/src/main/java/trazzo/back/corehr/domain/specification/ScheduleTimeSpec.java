package trazzo.back.corehr.domain.specification;

import java.time.LocalTime;

public class ScheduleTimeSpec {

    public boolean isValidScheduleTime(LocalTime entryTime, LocalTime departureTime) {
        if (entryTime == null || departureTime == null) {
            return false;
        }
        // Departure must differ from entry; nocturnal shifts that cross midnight
        // (e.g. 22:00 -> 06:00) are valid. The classic morning-to-evening shift
        // (entry < departure) is also valid.
        return !entryTime.equals(departureTime);
    }
}
