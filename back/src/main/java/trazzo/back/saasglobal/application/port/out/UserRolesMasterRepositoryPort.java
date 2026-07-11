package trazzo.back.saasglobal.application.port.out;

import java.util.List;

public interface UserRolesMasterRepositoryPort {
    List<Integer> findRoleIdsForUser(String userId);
    void replaceForUser(String userId, List<Integer> roleIds);
}
