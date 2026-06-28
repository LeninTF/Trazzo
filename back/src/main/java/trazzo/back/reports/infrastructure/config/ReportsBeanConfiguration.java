package trazzo.back.reports.infrastructure.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.reports.application.ports.in.CreateMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureDetailUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.in.GetMonthlyReportUseCase;
import trazzo.back.reports.application.ports.in.ListMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EventPublisherPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.application.ports.out.ReportGenerationPort;
import trazzo.back.reports.application.usecase.CreateMonthlyClosureService;
import trazzo.back.reports.application.usecase.GetMonthlyClosureDetailService;
import trazzo.back.reports.application.usecase.GetMonthlyClosureService;
import trazzo.back.reports.application.usecase.GetMonthlyReportService;
import trazzo.back.reports.application.usecase.ListMonthlyClosureService;
import trazzo.back.reports.infrastructure.adapters.out.attendance.EmployeeAttendanceSummaryAdapter;
import trazzo.back.reports.infrastructure.adapters.out.messaging.SpringEventPublisherAdapter;
import trazzo.back.reports.infrastructure.adapters.out.persistence.repository.MonthlyClosureDetailJdbcRepository;
import trazzo.back.reports.infrastructure.adapters.out.persistence.repository.MonthlyClosureJdbcRepository;
import trazzo.back.reports.infrastructure.adapters.out.reporting.ReportGenerationAdapter;

@Configuration
public class ReportsBeanConfiguration {

    @Bean
    public MonthlyClosureRepositoryPort monthlyClosureRepositoryPort(JdbcTemplate jdbcTemplate) {
        return new MonthlyClosureJdbcRepository(jdbcTemplate);
    }

    @Bean
    public MonthlyClosureDetailRepositoryPort monthlyClosureDetailRepositoryPort(JdbcTemplate jdbcTemplate) {
        return new MonthlyClosureDetailJdbcRepository(jdbcTemplate);
    }

    @Bean
    public EventPublisherPort eventPublisherPort(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringEventPublisherAdapter(applicationEventPublisher);
    }

    @Bean
    public ReportGenerationPort reportGenerationPort() {
        return new ReportGenerationAdapter();
    }

    @Bean
    public EmployeeAttendanceSummaryPort employeeAttendanceSummaryPort(JdbcTemplate jdbcTemplate) {
        return new EmployeeAttendanceSummaryAdapter(jdbcTemplate);
    }

    @Bean
    public CreateMonthlyClosureUseCase createMonthlyClosureUseCase(
            MonthlyClosureRepositoryPort closureRepository,
            MonthlyClosureDetailRepositoryPort detailRepository,
            ReportGenerationPort reportGenerationPort,
            EmployeeAttendanceSummaryPort attendanceSummaryPort,
            EventPublisherPort eventPublisher) {
        return new CreateMonthlyClosureService(
                closureRepository, detailRepository,
                reportGenerationPort, attendanceSummaryPort, eventPublisher);
    }

    @Bean
    public GetMonthlyClosureUseCase getMonthlyClosureUseCase(
            MonthlyClosureRepositoryPort closureRepository) {
        return new GetMonthlyClosureService(closureRepository);
    }

    @Bean
    public GetMonthlyClosureDetailUseCase getMonthlyClosureDetailUseCase(
            MonthlyClosureDetailRepositoryPort detailRepository) {
        return new GetMonthlyClosureDetailService(detailRepository);
    }

    @Bean
    public GetMonthlyReportUseCase getMonthlyReportUseCase(
            MonthlyClosureRepositoryPort closureRepository,
            MonthlyClosureDetailRepositoryPort detailRepository) {
        return new GetMonthlyReportService(closureRepository, detailRepository);
    }

    @Bean
    public ListMonthlyClosureUseCase listMonthlyClosureUseCase(
            MonthlyClosureRepositoryPort closureRepository) {
        return new ListMonthlyClosureService(closureRepository);
    }
}
