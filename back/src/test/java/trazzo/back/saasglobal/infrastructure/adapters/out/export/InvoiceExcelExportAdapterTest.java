package trazzo.back.saasglobal.infrastructure.adapters.out.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;

class InvoiceExcelExportAdapterTest {

    private final InvoiceExcelExportAdapter adapter = new InvoiceExcelExportAdapter();

    private static InvoiceResult invoice(String series) {
        return new InvoiceResult("inv-1", "tenant-1", series, "001", "01_FACTURA",
                "20222222222", "Cliente SAC", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(11),
                "PENDIENTE", null, LocalDateTime.now());
    }

    @Test
    void toExcel_producesWorkbookWithHeaderAndOneRowPerInvoice() throws Exception {
        byte[] bytes = adapter.toExcel(List.of(invoice("F001"), invoice("F002")));

        assertTrue(bytes.length > 0);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(3, sheet.getPhysicalNumberOfRows());
            Row header = sheet.getRow(0);
            assertEquals("Serie", header.getCell(0).getStringCellValue());
            Row firstDataRow = sheet.getRow(1);
            assertEquals("F001", firstDataRow.getCell(0).getStringCellValue());
        }
    }

    @Test
    void toExcel_producesHeaderOnlyWhenEmpty() throws Exception {
        byte[] bytes = adapter.toExcel(List.of());

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getPhysicalNumberOfRows());
        }
    }
}
