package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RolePermissionsEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RolePermissionsId;

import java.util.List;

@Repository
public interface RolePermissionsJpaRepository extends JpaRepository<RolePermissionsEntity, RolePermissionsId> {

    List<RolePermissionsEntity> findByIdRoleId(String roleId);

    boolean existsByIdRoleIdAndIdPermissionId(String roleId, String permissionId);

    void deleteByIdRoleIdAndIdPermissionId(String roleId, String permissionId);
}
