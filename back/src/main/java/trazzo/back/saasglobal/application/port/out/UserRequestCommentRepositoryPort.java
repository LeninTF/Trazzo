package trazzo.back.saasglobal.application.port.out;

import java.util.Optional;
import trazzo.back.saasglobal.domain.model.request.UserRequestComment;

public interface UserRequestCommentRepositoryPort {
    UserRequestComment save(UserRequestComment userRequestComment);
    Optional<UserRequestComment> findByRequestCommentId(Integer requestCommentId);
}
