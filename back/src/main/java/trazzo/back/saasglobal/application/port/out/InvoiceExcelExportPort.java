package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import trazzo.back.saasglobal.application.dto.result.InvoiceResult;

public interface InvoiceExcelExportPort {
    byte[] toExcel(List<InvoiceResult> rows);
}
