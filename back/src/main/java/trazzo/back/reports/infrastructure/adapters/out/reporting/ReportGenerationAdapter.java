package trazzo.back.reports.infrastructure.adapters.out.reporting;

import trazzo.back.reports.application.ports.out.ReportGenerationPort;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.util.List;

public class ReportGenerationAdapter implements ReportGenerationPort {

    @Override
    public String generateExcelReport(MonthlyClosure closure, List<MonthlyClosureDetail> details) {
        return "/reports/excel/" + closure.getId() + ".xlsx";
    }

    @Override
    public String generatePdfReport(MonthlyClosure closure, List<MonthlyClosureDetail> details) {
        return "/reports/pdf/" + closure.getId() + ".pdf";
    }
}
