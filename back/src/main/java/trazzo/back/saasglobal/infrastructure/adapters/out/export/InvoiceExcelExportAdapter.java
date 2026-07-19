package trazzo.back.saasglobal.infrastructure.adapters.out.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.port.out.InvoiceExcelExportPort;

@Component
public class InvoiceExcelExportAdapter implements InvoiceExcelExportPort {

    private static final String[] HEADERS = {
            "Serie", "Numero", "Tipo", "RUC Cliente", "Cliente",
            "Subtotal", "IGV", "Total", "Estado", "Fecha"
    };

    @Override
    public byte[] toExcel(List<InvoiceResult> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Facturas");
            writeHeader(sheet);
            writeRows(sheet, rows);
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate invoices Excel export", e);
        }
    }

    private void writeHeader(XSSFSheet sheet) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }
    }

    private void writeRows(XSSFSheet sheet, List<InvoiceResult> rows) {
        int rowNum = 1;
        for (InvoiceResult invoice : rows) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(invoice.invoiceSeries());
            row.createCell(1).setCellValue(invoice.consecutiveNumber());
            row.createCell(2).setCellValue(invoice.voucherType());
            row.createCell(3).setCellValue(invoice.clientTaxId());
            row.createCell(4).setCellValue(invoice.clientName());
            row.createCell(5).setCellValue(invoice.subTotal() != null ? invoice.subTotal().doubleValue() : 0);
            row.createCell(6).setCellValue(invoice.taxAmount() != null ? invoice.taxAmount().doubleValue() : 0);
            row.createCell(7).setCellValue(invoice.total() != null ? invoice.total().doubleValue() : 0);
            row.createCell(8).setCellValue(invoice.paymentStatus());
            row.createCell(9).setCellValue(invoice.createdAt() != null ? invoice.createdAt().toString() : "");
        }
    }
}
