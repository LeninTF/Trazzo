package trazzo.back.dashboard.application.port.out;

import java.time.LocalDate;
import java.util.List;

public interface DashboardAttendancePort {

    long countByStateAndDateRange(String state, LocalDate desde, LocalDate hasta);

    long countTotalByDateRange(LocalDate desde, LocalDate hasta);

    long countPunctualByYear(int year);

    long countTotalByYear(int year);

    List<RolePunctualityProjection> getPunctualityByRole(LocalDate desde, LocalDate hasta);

    long countActiveUsers();

    List<Long> getUserIdsWithoutAttendance(LocalDate date);

    record RolePunctualityProjection(String roleName, long total, long punctual) {
    }
}
