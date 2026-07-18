package trazzo.back.saasglobal.application.port.in;

import java.util.List;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;
import trazzo.back.saasglobal.application.dto.result.PaginatedResult;

public interface InvoiceUseCase {
    PaginatedResult<InvoiceResult> listAll(String paymentStatus, String tenantId, String dateFrom, String dateTo,
                                            int page, int size);
    InvoiceResult getById(String id);
    List<InvoiceResult> listAllMatching(String paymentStatus, String tenantId, String dateFrom, String dateTo);
}
