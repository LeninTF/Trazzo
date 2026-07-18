package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.result.PaginatedResult;
import trazzo.back.saasglobal.application.dto.result.SubscriptionResult;

public interface SubscriptionUseCase {
    PaginatedResult<SubscriptionResult> listAll(int page, int size);
}
