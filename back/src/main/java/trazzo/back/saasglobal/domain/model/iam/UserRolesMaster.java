package trazzo.back.saasglobal.domain.model.iam;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class UserRolesMaster {

    private final String userId;
    private final Integer rolesMasterId;
    private final LocalDateTime createdAt;

    private UserRolesMaster(String userId, Integer rolesMasterId, LocalDateTime createdAt) {
        this.userId = userId;
        this.rolesMasterId = rolesMasterId;
        this.createdAt = createdAt;
    }

    public static UserRolesMaster assign(String userId, Integer rolesMasterId) {
        return new UserRolesMaster(userId, rolesMasterId, LocalDateTime.now(Clock.systemDefaultZone()));
    }

    public static UserRolesMaster restore(String userId, Integer rolesMasterId, LocalDateTime createdAt) {
        return new UserRolesMaster(userId, rolesMasterId, createdAt);
    }
}
