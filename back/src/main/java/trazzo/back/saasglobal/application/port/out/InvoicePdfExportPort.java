package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;

public interface InvoicePdfExportPort {
    byte[] toPdf(List<InvoiceResult> rows);
}
