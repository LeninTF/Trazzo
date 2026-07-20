package trazzo.back.saasglobal.application.usecase;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.port.in.InvoiceUseCase;
import trazzo.back.saasglobal.application.port.out.InvoiceRepositoryPort;
import trazzo.back.saasglobal.domain.model.invoice.Invoice;

@Service
@RequiredArgsConstructor
public class InvoiceService implements InvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;

    @Override
    public PaginatedResult<InvoiceResult> listAll(String paymentStatus, String tenantId, String dateFrom,
                                                   String dateTo, int page, int size) {
        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        List<InvoiceResult> results = invoiceRepository.findByFilters(paymentStatus, tenantId, from, to, page, size)
                .stream().map(this::toResult).toList();
        long total = invoiceRepository.countByFilters(paymentStatus, tenantId, from, to);
        return PaginatedResult.of(results, page, size, total);
    }

    @Override
    public InvoiceResult getById(String id) {
        return invoiceRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    @Override
    public List<InvoiceResult> listAllMatching(String paymentStatus, String tenantId, String dateFrom, String dateTo) {
        return invoiceRepository.findAllMatching(paymentStatus, tenantId, parseDate(dateFrom), parseDate(dateTo))
                .stream().map(this::toResult).toList();
    }

    private static LocalDate parseDate(String value) {
        return value != null && !value.isBlank() ? LocalDate.parse(value) : null;
    }

    private InvoiceResult toResult(Invoice invoice) {
        return new InvoiceResult(invoice.getId(), invoice.getTenantId(), invoice.getInvoiceSeries(),
                invoice.getConsecutiveNumber(), invoice.getVoucherType(), invoice.getClientTaxId(),
                invoice.getClientName(), invoice.getSubTotal(), invoice.getTaxAmount(), invoice.getTotal(),
                invoice.getPaymentStatus(), invoice.getExpirationDate(), invoice.getCreatedAt());
    }
}
