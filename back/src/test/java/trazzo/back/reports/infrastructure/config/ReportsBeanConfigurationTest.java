package trazzo.back.reports.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.corehr.application.port.in.CoreHrAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EventPublisherPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.application.ports.out.ReportGenerationPort;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ReportsBeanConfigurationTest {

    private final ReportsBeanConfiguration config = new ReportsBeanConfiguration();

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private CoreHrAttendanceSummaryPort coreHrAttendanceSummaryPort;
    @Mock private MonthlyClosureRepositoryPort closureRepo;
    @Mock private MonthlyClosureDetailRepositoryPort detailRepo;
    @Mock private ReportGenerationPort reportGenerationPort;
    @Mock private EmployeeAttendanceSummaryPort attendanceSummaryPort;
    @Mock private EventPublisherPort eventPublisher;

    @Test
    void shouldCreateMonthlyClosureRepositoryPort() {
        assertNotNull(config.monthlyClosureRepositoryPort(jdbcTemplate));
    }

    @Test
    void shouldCreateMonthlyClosureDetailRepositoryPort() {
        assertNotNull(config.monthlyClosureDetailRepositoryPort(jdbcTemplate));
    }

    @Test
    void shouldCreateEventPublisherPort() {
        assertNotNull(config.eventPublisherPort(applicationEventPublisher));
    }

    @Test
    void shouldCreateReportGenerationPort() {
        assertNotNull(config.reportGenerationPort());
    }

    @Test
    void shouldCreateEmployeeAttendanceSummaryPort() {
        assertNotNull(config.employeeAttendanceSummaryPort(coreHrAttendanceSummaryPort));
    }

    @Test
    void shouldCreateCreateMonthlyClosureUseCase() {
        assertNotNull(config.createMonthlyClosureUseCase(closureRepo, detailRepo, reportGenerationPort, attendanceSummaryPort, eventPublisher));
    }

    @Test
    void shouldCreateGetMonthlyClosureUseCase() {
        assertNotNull(config.getMonthlyClosureUseCase(closureRepo));
    }

    @Test
    void shouldCreateGetMonthlyClosureDetailUseCase() {
        assertNotNull(config.getMonthlyClosureDetailUseCase(detailRepo));
    }

    @Test
    void shouldCreateGetMonthlyReportUseCase() {
        assertNotNull(config.getMonthlyReportUseCase(closureRepo, detailRepo));
    }

    @Test
    void shouldCreateListMonthlyClosureUseCase() {
        assertNotNull(config.listMonthlyClosureUseCase(closureRepo));
    }
}
