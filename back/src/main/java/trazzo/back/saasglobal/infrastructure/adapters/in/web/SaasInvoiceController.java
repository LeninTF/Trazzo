package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.port.in.InvoiceUseCase;
import trazzo.back.saasglobal.application.port.out.InvoiceExcelExportPort;
import trazzo.back.saasglobal.application.port.out.InvoicePdfExportPort;

@RestController
@RequestMapping("/saas/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('billing-suscripciones.historial-facturacion')")
public class SaasInvoiceController {

    private final InvoiceUseCase invoiceUseCase;
    private final InvoiceExcelExportPort excelExportPort;
    private final InvoicePdfExportPort pdfExportPort;

    @GetMapping
    public ResponseEntity<PaginatedResult<InvoiceResult>> listAll(
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(invoiceUseCase.listAll(paymentStatus, tenantId, dateFrom, dateTo, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResult> getById(@PathVariable String id) {
        return ResponseEntity.ok(invoiceUseCase.getById(id));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        var rows = invoiceUseCase.listAllMatching(paymentStatus, tenantId, dateFrom, dateTo);
        byte[] content = excelExportPort.toExcel(rows);
        return export(content, "facturas.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        var rows = invoiceUseCase.listAllMatching(paymentStatus, tenantId, dateFrom, dateTo);
        byte[] content = pdfExportPort.toPdf(rows);
        return export(content, "facturas.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> export(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
