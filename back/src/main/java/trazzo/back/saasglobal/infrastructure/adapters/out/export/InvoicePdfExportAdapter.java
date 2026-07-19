package trazzo.back.saasglobal.infrastructure.adapters.out.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.port.out.InvoicePdfExportPort;

@Component
public class InvoicePdfExportAdapter implements InvoicePdfExportPort {

    private static final String[] HEADERS = {
            "Serie", "Numero", "Tipo", "RUC Cliente", "Cliente",
            "Subtotal", "IGV", "Total", "Estado", "Fecha"
    };

    @Override
    public byte[] toPdf(List<InvoiceResult> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Facturas"));
            document.add(buildTable(rows));
            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate invoices PDF export", e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate invoices PDF export", e);
        }
    }

    private PdfPTable buildTable(List<InvoiceResult> rows) {
        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        for (String header : HEADERS) {
            table.addCell(new PdfPCell(new Paragraph(header)));
        }
        for (InvoiceResult invoice : rows) {
            table.addCell(cell(invoice.invoiceSeries()));
            table.addCell(cell(invoice.consecutiveNumber()));
            table.addCell(cell(invoice.voucherType()));
            table.addCell(cell(invoice.clientTaxId()));
            table.addCell(cell(invoice.clientName()));
            table.addCell(cell(invoice.subTotal() != null ? invoice.subTotal().toPlainString() : ""));
            table.addCell(cell(invoice.taxAmount() != null ? invoice.taxAmount().toPlainString() : ""));
            table.addCell(cell(invoice.total() != null ? invoice.total().toPlainString() : ""));
            table.addCell(cell(invoice.paymentStatus()));
            table.addCell(cell(invoice.createdAt() != null ? invoice.createdAt().toString() : ""));
        }
        return table;
    }

    private PdfPCell cell(String value) {
        return new PdfPCell(new Paragraph(value != null ? value : ""));
    }
}
