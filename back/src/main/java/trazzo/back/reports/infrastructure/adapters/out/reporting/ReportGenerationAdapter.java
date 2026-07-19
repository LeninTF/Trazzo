package trazzo.back.reports.infrastructure.adapters.out.reporting;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import trazzo.back.reports.application.ports.out.ReportGenerationPort;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class ReportGenerationAdapter implements ReportGenerationPort {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final FileStoragePort fileStoragePort;

    public ReportGenerationAdapter(FileStoragePort fileStoragePort) {
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public String generateExcelReport(MonthlyClosure closure, List<MonthlyClosureDetail> details) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Reporte " + closure.getMonth() + "-" + closure.getYear());

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            String[] columns = {"Empleado", "Documento", "Departamento", "Rol",
                    "Horas Trabajadas", "Minutos Tardanza", "Ausencias", "Horas Extras"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (MonthlyClosureDetail detail : details) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(detail.getTenantUserFullName());
                row.createCell(1).setCellValue(detail.getTenantUserDocument());
                row.createCell(2).setCellValue(detail.getDepartmentName());
                row.createCell(3).setCellValue(detail.getRoleName());
                row.createCell(4).setCellValue(detail.getTotalWorkedHours());
                row.createCell(5).setCellValue(detail.getTotalTardinessMinutes());
                row.createCell(6).setCellValue(detail.getTotalAbsences());
                row.createCell(7).setCellValue(detail.getTotalOvertimeHours());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String objectKey = "reports/monthly/" + closure.getYear() + "/" + closure.getMonth()
                    + "/" + closure.getId() + "-excel.xlsx";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            baos.flush();

            fileStoragePort.uploadFile(objectKey, new ByteArrayInputStream(baos.toByteArray()),
                    baos.size(), EXCEL_CONTENT_TYPE);

            return fileStoragePort.buildPublicUrl(objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    @Override
    public String generatePdfReport(MonthlyClosure closure, List<MonthlyClosureDetail> details) {
        Document document = new Document();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 16, com.lowagie.text.Font.BOLD);
            document.add(new Paragraph("Reporte Mensual - " + closure.getMonth() + "/" + closure.getYear(), titleFont));
            document.add(new Paragraph("Total empleados: " + closure.getTotalEmployees()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            String[] headers = {"Empleado", "Documento", "Departamento", "Rol",
                    "Horas Trab.", "Min. Tard.", "Ausencias", "H. Extras"};
            for (String header : headers) {
                table.addCell(header);
            }

            for (MonthlyClosureDetail detail : details) {
                table.addCell(detail.getTenantUserFullName());
                table.addCell(detail.getTenantUserDocument());
                table.addCell(detail.getDepartmentName());
                table.addCell(detail.getRoleName());
                table.addCell(String.valueOf(detail.getTotalWorkedHours()));
                table.addCell(String.valueOf(detail.getTotalTardinessMinutes()));
                table.addCell(String.valueOf(detail.getTotalAbsences()));
                table.addCell(String.valueOf(detail.getTotalOvertimeHours()));
            }

            document.add(table);
            document.close();

            String objectKey = "reports/monthly/" + closure.getYear() + "/" + closure.getMonth()
                    + "/" + closure.getId() + "-report.pdf";

            fileStoragePort.uploadFile(objectKey, new ByteArrayInputStream(baos.toByteArray()),
                    baos.size(), PDF_CONTENT_TYPE);

            return fileStoragePort.buildPublicUrl(objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }
}
