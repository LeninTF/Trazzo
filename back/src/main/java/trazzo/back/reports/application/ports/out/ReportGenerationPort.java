package trazzo.back.reports.application.ports.out;

import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.List;

public interface ReportGenerationPort {
    String generateExcelReport(MonthlyClosure closure, List<MonthlyClosureDetail> details);
    String generatePdfReport(MonthlyClosure closure, List<MonthlyClosureDetail> details);
}
