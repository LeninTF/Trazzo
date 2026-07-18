package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import trazzo.back.saasglobal.domain.model.request.RequestComments;

public interface RequestCommentRepositoryPort {
    RequestComments save(RequestComments comment);
    List<RequestComments> findByRequestId(Integer requestId);
}
