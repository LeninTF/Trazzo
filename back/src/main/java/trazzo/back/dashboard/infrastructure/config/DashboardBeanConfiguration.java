package trazzo.back.dashboard.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.dashboard.application.port.in.DashboardAlertsUseCase;
import trazzo.back.dashboard.application.port.in.DashboardSummaryUseCase;
import trazzo.back.dashboard.application.port.in.PuntualidadRolUseCase;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;
import trazzo.back.dashboard.application.port.out.DashboardIncidentPort;
import trazzo.back.dashboard.application.usecase.DashboardAlertsService;
import trazzo.back.dashboard.application.usecase.DashboardSummaryService;
import trazzo.back.dashboard.application.usecase.PuntualidadRolService;
import trazzo.back.dashboard.infrastructure.adapters.out.persistence.DashboardAttendanceJdbcAdapter;
import trazzo.back.dashboard.infrastructure.adapters.out.persistence.DashboardIncidentJdbcAdapter;

@Configuration
public class DashboardBeanConfiguration {

    @Bean
    public DashboardAttendancePort dashboardAttendancePort(JdbcTemplate jdbcTemplate) {
        return new DashboardAttendanceJdbcAdapter(jdbcTemplate);
    }

    @Bean
    public DashboardIncidentPort dashboardIncidentPort(JdbcTemplate jdbcTemplate) {
        return new DashboardIncidentJdbcAdapter(jdbcTemplate);
    }

    @Bean
    public DashboardSummaryUseCase dashboardSummaryUseCase(
            DashboardAttendancePort attendancePort,
            DashboardIncidentPort incidentPort) {
        return new DashboardSummaryService(attendancePort, incidentPort);
    }

    @Bean
    public PuntualidadRolUseCase puntualidadRolUseCase(DashboardAttendancePort attendancePort) {
        return new PuntualidadRolService(attendancePort);
    }

    @Bean
    public DashboardAlertsUseCase dashboardAlertsUseCase(DashboardAttendancePort attendancePort) {
        return new DashboardAlertsService(attendancePort);
    }
}
