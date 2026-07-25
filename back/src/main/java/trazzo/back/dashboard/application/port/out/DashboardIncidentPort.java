package trazzo.back.dashboard.application.port.out;

import java.time.LocalDateTime;

public interface DashboardIncidentPort {
    long countByDateRange(LocalDateTime desde, LocalDateTime hasta);
}
