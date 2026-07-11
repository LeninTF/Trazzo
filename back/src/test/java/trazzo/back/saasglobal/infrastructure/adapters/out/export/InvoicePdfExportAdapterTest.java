package trazzo.back.saasglobal.infrastructure.adapters.out.export;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;

class InvoicePdfExportAdapterTest {

    private final InvoicePdfExportAdapter adapter = new InvoicePdfExportAdapter();

    private static InvoiceResult invoice(String series) {
        return new InvoiceResult("inv-1", "tenant-1", series, "001", "01_FACTURA",
                "20222222222", "Cliente SAC", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(11),
                "PENDIENTE", null, LocalDateTime.now());
    }

    @Test
    void toPdf_producesValidPdfBytes() {
        byte[] bytes = adapter.toPdf(List.of(invoice("F001")));

        assertTrue(bytes.length > 0);
        String header = new String(bytes, 0, 5, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-", header);
    }

    @Test
    void toPdf_producesValidPdfBytesWhenEmpty() {
        byte[] bytes = adapter.toPdf(List.of());

        assertTrue(bytes.length > 0);
        String header = new String(bytes, 0, 5, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF-", header);
    }
}
