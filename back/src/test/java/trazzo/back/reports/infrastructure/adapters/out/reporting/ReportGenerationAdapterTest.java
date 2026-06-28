package trazzo.back.reports.infrastructure.adapters.out.reporting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class ReportGenerationAdapterTest {

    private final ReportGenerationAdapter adapter = new ReportGenerationAdapter();

    @Test
    void shouldGenerateExcelReportUrl() {
        MonthlyClosure closure = new MonthlyClosure(
                UUID.randomUUID(), 6, 2025, 10, null, null, UUID.randomUUID(), LocalDateTime.now());
        List<MonthlyClosureDetail> details = List.of();

        String url = adapter.generateExcelReport(closure, details);

        assertTrue(url.startsWith("/reports/excel/"));
        assertTrue(url.endsWith(".xlsx"));
        assertTrue(url.contains(closure.getId().toString()));
    }

    @Test
    void shouldGeneratePdfReportUrl() {
        MonthlyClosure closure = new MonthlyClosure(
                UUID.randomUUID(), 6, 2025, 10, null, null, UUID.randomUUID(), LocalDateTime.now());
        List<MonthlyClosureDetail> details = List.of();

        String url = adapter.generatePdfReport(closure, details);

        assertTrue(url.startsWith("/reports/pdf/"));
        assertTrue(url.endsWith(".pdf"));
        assertTrue(url.contains(closure.getId().toString()));
    }

    @Test
    void shouldGenerateDifferentUrlsForDifferentClosures() {
        MonthlyClosure c1 = new MonthlyClosure(
                UUID.randomUUID(), 6, 2025, 10, null, null, UUID.randomUUID(), LocalDateTime.now());
        MonthlyClosure c2 = new MonthlyClosure(
                UUID.randomUUID(), 7, 2025, 5, null, null, UUID.randomUUID(), LocalDateTime.now());

        String excel1 = adapter.generateExcelReport(c1, List.of());
        String excel2 = adapter.generateExcelReport(c2, List.of());

        assertNotEquals(excel1, excel2);
    }
}
