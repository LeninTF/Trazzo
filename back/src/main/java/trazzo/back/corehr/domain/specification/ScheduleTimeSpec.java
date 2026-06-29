package trazzo.back.corehr.domain.specification;

import java.time.LocalTime;

public class ScheduleTimeSpec {

    public boolean isValidScheduleTime(LocalTime entryTime, LocalTime departureTime) {
        if (entryTime == null || departureTime == null) {
            return false;
        }
        return entryTime.isBefore(departureTime);
    }
}
