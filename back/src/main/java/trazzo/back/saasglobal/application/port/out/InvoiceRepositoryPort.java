package trazzo.back.saasglobal.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.invoice.Invoice;

public interface InvoiceRepositoryPort {
    List<Invoice> findByFilters(String paymentStatus, String tenantId, LocalDate dateFrom, LocalDate dateTo,
                                 int page, int size);
    long countByFilters(String paymentStatus, String tenantId, LocalDate dateFrom, LocalDate dateTo);
    List<Invoice> findAllMatching(String paymentStatus, String tenantId, LocalDate dateFrom, LocalDate dateTo);
    Optional<Invoice> findById(String id);
}
