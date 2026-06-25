package trazzo.back.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import trazzo.back.reports.application.dto.command.CreateMonthlyClosureCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;
import trazzo.back.reports.application.ports.out.EventPublisherPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.application.ports.out.ReportGenerationPort;
import trazzo.back.reports.domain.exception.DuplicateClosureException;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CreateMonthlyClosureServiceTest {

    @Mock
    private MonthlyClosureRepositoryPort closureRepository;
    @Mock
    private MonthlyClosureDetailRepositoryPort detailRepository;
    @Mock
    private ReportGenerationPort reportGenerationPort;
    @Mock
    private EmployeeAttendanceSummaryPort attendanceSummaryPort;
    @Mock
    private EventPublisherPort eventPublisher;

    private CreateMonthlyClosureService service;

    @BeforeEach
    void setUp() {
        service = new CreateMonthlyClosureService(
                closureRepository, detailRepository,
                reportGenerationPort, attendanceSummaryPort, eventPublisher);
    }

    @Test
    void shouldCreateMonthlyClosureSuccessfully() {
        when(closureRepository.findByMonthAndYear(6, 2025)).thenReturn(List.of());

        EmployeeMonthlySummary summary = new EmployeeMonthlySummary(
                "user-1", "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10.0, 1, 5.0);
        when(attendanceSummaryPort.getMonthlySummaries(6, 2025))
                .thenReturn(List.of(summary));
        when(reportGenerationPort.generateExcelReport(any(MonthlyClosure.class), anyList()))
                .thenReturn("excel-url");
        when(reportGenerationPort.generatePdfReport(any(MonthlyClosure.class), anyList()))
                .thenReturn("pdf-url");
        when(closureRepository.save(any(MonthlyClosure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, "creator-1");
        MonthlyClosureResult result = service.execute(command);

        assertNotNull(result);
        assertEquals(6, result.month());
        assertEquals(2025, result.year());
        assertEquals(1, result.totalEmployees());
        assertEquals("excel-url", result.excelReportUrl());
        assertEquals("pdf-url", result.pdfReportUrl());
        assertNotNull(result.createdAt());

        verify(closureRepository).findByMonthAndYear(6, 2025);
        verify(attendanceSummaryPort).getMonthlySummaries(6, 2025);
        verify(closureRepository).save(any(MonthlyClosure.class));
        verify(detailRepository).saveAll(anyList());
        verify(reportGenerationPort).generateExcelReport(any(MonthlyClosure.class), anyList());
        verify(reportGenerationPort).generatePdfReport(any(MonthlyClosure.class), anyList());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldCreateWithMultipleEmployees() {
        when(closureRepository.findByMonthAndYear(6, 2025)).thenReturn(List.of());
        EmployeeMonthlySummary emp1 = new EmployeeMonthlySummary(
                "u1", "Juan", "111", "TI", "Dev", 160.0, 5.0, 0, 10.0);
        EmployeeMonthlySummary emp2 = new EmployeeMonthlySummary(
                "u2", "Ana", "222", "HR", "Mgr", 80.0, 15.0, 2, 0.0);
        when(attendanceSummaryPort.getMonthlySummaries(6, 2025))
                .thenReturn(List.of(emp1, emp2));
        when(reportGenerationPort.generateExcelReport(any(MonthlyClosure.class), anyList()))
                .thenReturn("excel-url");
        when(reportGenerationPort.generatePdfReport(any(MonthlyClosure.class), anyList()))
                .thenReturn("pdf-url");
        when(closureRepository.save(any(MonthlyClosure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, "creator-1");
        MonthlyClosureResult result = service.execute(command);

        assertEquals(2, result.totalEmployees());
        verify(detailRepository).saveAll(argThat(details -> ((List<MonthlyClosureDetail>) details).size() == 2));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldHandleEmptyEmployeeList() {
        when(closureRepository.findByMonthAndYear(6, 2025)).thenReturn(List.of());
        when(attendanceSummaryPort.getMonthlySummaries(6, 2025))
                .thenReturn(List.of());
        when(reportGenerationPort.generateExcelReport(any(MonthlyClosure.class), anyList()))
                .thenReturn("excel-url");
        when(reportGenerationPort.generatePdfReport(any(MonthlyClosure.class), anyList()))
                .thenReturn("pdf-url");
        when(closureRepository.save(any(MonthlyClosure.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, "creator-1");
        MonthlyClosureResult result = service.execute(command);

        assertEquals(0, result.totalEmployees());
        verify(detailRepository).saveAll(argThat(details -> ((List<MonthlyClosureDetail>) details).isEmpty()));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenDuplicateClosureExists() {
        when(closureRepository.findByMonthAndYear(6, 2025))
                .thenReturn(List.of(new MonthlyClosure(
                        UUID.randomUUID(), 6, 2025, 10, null, null, "user", LocalDateTime.now())));

        CreateMonthlyClosureCommand command = new CreateMonthlyClosureCommand(6, 2025, "creator-1");

        assertThrows(DuplicateClosureException.class, () -> service.execute(command));

        verify(closureRepository).findByMonthAndYear(6, 2025);
        verifyNoInteractions(attendanceSummaryPort);
        verifyNoInteractions(eventPublisher);
    }
}
