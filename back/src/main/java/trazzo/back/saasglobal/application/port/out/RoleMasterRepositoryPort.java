package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

public interface RoleMasterRepositoryPort {
    RoleMaster save(RoleMaster role);
    Optional<RoleMaster> findById(Integer id);
    Optional<RoleMaster> findByName(String name);
    List<RoleMaster> findAll();
    void deleteById(Integer id);
    void replacePermissions(Integer roleId, List<String> permissionCodes);
    boolean isAssignedToAnyUser(Integer roleId);
}
