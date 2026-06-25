package trazzo.back.reports.application.usecase;

import trazzo.back.reports.application.dto.command.CreateMonthlyClosureCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.in.CreateMonthlyClosureUseCase;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;
import trazzo.back.reports.application.ports.out.EventPublisherPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.application.ports.out.ReportGenerationPort;
import trazzo.back.reports.domain.event.MonthlyClosureCreatedEvent;
import trazzo.back.reports.domain.exception.DuplicateClosureException;
import trazzo.back.reports.domain.model.closure.ClosurePeriod;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CreateMonthlyClosureService implements CreateMonthlyClosureUseCase {

    private final MonthlyClosureRepositoryPort closureRepository;
    private final MonthlyClosureDetailRepositoryPort detailRepository;
    private final ReportGenerationPort reportGenerationPort;
    private final EmployeeAttendanceSummaryPort attendanceSummaryPort;
    private final EventPublisherPort eventPublisher;

    public CreateMonthlyClosureService(MonthlyClosureRepositoryPort closureRepository,
                                       MonthlyClosureDetailRepositoryPort detailRepository,
                                       ReportGenerationPort reportGenerationPort,
                                       EmployeeAttendanceSummaryPort attendanceSummaryPort,
                                       EventPublisherPort eventPublisher) {
        this.closureRepository = closureRepository;
        this.detailRepository = detailRepository;
        this.reportGenerationPort = reportGenerationPort;
        this.attendanceSummaryPort = attendanceSummaryPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public MonthlyClosureResult execute(CreateMonthlyClosureCommand command) {
        ClosurePeriod period = new ClosurePeriod(command.month(), command.year());

        List<MonthlyClosure> existing = closureRepository.findByMonthAndYear(period.month(), period.year());
        if (!existing.isEmpty()) {
            throw new DuplicateClosureException(period.month(), period.year());
        }

        List<EmployeeMonthlySummary> summaries = attendanceSummaryPort.getMonthlySummaries(
                period.month(), period.year());

        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        List<MonthlyClosureDetail> details = summaries.stream()
                .map(s -> new MonthlyClosureDetail(
                        UUID.randomUUID(),
                        closureId,
                        s.tenantUserId(),
                        s.tenantUserFullName(),
                        s.tenantUserDocument(),
                        s.departmentName(),
                        s.roleName(),
                        s.totalWorkedHours(),
                        s.totalTardinessMinutes(),
                        s.totalAbsences(),
                        s.totalOvertimeHours(),
                        now))
                .toList();

        MonthlyClosure closure = new MonthlyClosure(
                closureId, period.month(), period.year(),
                summaries.size(), null, null,
                command.createdByUserId(), now);

        String excelUrl = reportGenerationPort.generateExcelReport(closure, details);
        String pdfUrl = reportGenerationPort.generatePdfReport(closure, details);

        MonthlyClosure finalClosure = closure.withReportUrls(excelUrl, pdfUrl);

        closureRepository.save(finalClosure);
        detailRepository.saveAll(details);

        eventPublisher.publishEvent(new MonthlyClosureCreatedEvent(closureId, period, command.createdByUserId(), now));

        return new MonthlyClosureResult(
                closureId, period.month(), period.year(),
                summaries.size(), excelUrl, pdfUrl, now);
    }
}
