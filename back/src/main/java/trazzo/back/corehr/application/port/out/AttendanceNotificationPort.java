package trazzo.back.corehr.application.port.out;

import java.time.LocalDate;

public interface AttendanceNotificationPort {
    void notifyTardanza(String toEmail, String workerName, int minutesLate, LocalDate date);
}
