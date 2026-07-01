package trazzo.back.audit.application.port.out;

import java.util.Optional;

public interface UserInfoPort {
    Optional<UserInfo> findByUserId(String userId);

    record UserInfo(String userId, String userName, String userEmail) {}
}
