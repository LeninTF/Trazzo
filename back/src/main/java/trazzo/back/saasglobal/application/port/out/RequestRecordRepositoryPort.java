package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import trazzo.back.saasglobal.domain.model.request.RequestRecord;

public interface RequestRecordRepositoryPort {
    RequestRecord save(RequestRecord record);
    List<RequestRecord> findByRequestId(Integer requestId);
}
