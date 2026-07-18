package trazzo.back.audit.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserInfoPort {
    Optional<UserInfo> findByUserId(String userId);

    Map<String, UserInfo> findByUserIds(List<String> userIds);

    record UserInfo(String userId, String userName, String userEmail) {}
}
