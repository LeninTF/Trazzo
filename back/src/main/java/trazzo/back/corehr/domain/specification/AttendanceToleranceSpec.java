package trazzo.back.corehr.domain.specification;

import java.time.LocalTime;
import java.util.List;
import trazzo.back.corehr.domain.model.ToleranciaType;

public class AttendanceToleranceSpec {

    public int calculateMinutesLate(LocalTime scheduledEntryTime, LocalTime actualCheckIn, List<Integer> toleranceMinutes) {
        if (scheduledEntryTime == null || actualCheckIn == null) {
            return 0;
        }
        if (!actualCheckIn.isAfter(scheduledEntryTime)) {
            return 0;
        }
        int effectiveTolerance = toleranceMinutes == null ? 0 : toleranceMinutes.stream()
                .filter(m -> m != null && m >= 0)
                .max(Integer::compareTo)
                .orElse(0);
        int diffMinutes = (int) java.time.Duration.between(scheduledEntryTime, actualCheckIn).toMinutes();
        int lateMinutes = diffMinutes - effectiveTolerance;
        return Math.max(lateMinutes, 0);
    }
}
