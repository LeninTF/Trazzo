package trazzo.back.reports.application.ports.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.List;

class ReportGenerationPortTest {

    @Test
    void shouldBeInterface() {
        assertTrue(ReportGenerationPort.class.isInterface());
    }

    @Test
    void shouldDefineGenerateExcelReportMethod() throws NoSuchMethodException {
        ReportGenerationPort.class.getDeclaredMethod("generateExcelReport", MonthlyClosure.class, List.class);
    }

    @Test
    void shouldDefineGeneratePdfReportMethod() throws NoSuchMethodException {
        ReportGenerationPort.class.getDeclaredMethod("generatePdfReport", MonthlyClosure.class, List.class);
    }

    @Test
    void shouldReturnStringFromGenerateExcelReport() throws NoSuchMethodException {
        var method = ReportGenerationPort.class.getDeclaredMethod("generateExcelReport", MonthlyClosure.class, List.class);
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    void shouldReturnStringFromGeneratePdfReport() throws NoSuchMethodException {
        var method = ReportGenerationPort.class.getDeclaredMethod("generatePdfReport", MonthlyClosure.class, List.class);
        assertEquals(String.class, method.getReturnType());
    }
}
