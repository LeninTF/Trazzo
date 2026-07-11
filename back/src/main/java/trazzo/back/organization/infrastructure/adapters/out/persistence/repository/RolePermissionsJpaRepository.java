package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RolePermissionsEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RolePermissionsId;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionsJpaRepository extends JpaRepository<RolePermissionsEntity, RolePermissionsId> {

    List<RolePermissionsEntity> findByIdRoleId(UUID roleId);

    boolean existsByIdRoleIdAndIdPermissionId(UUID roleId, UUID permissionId);

    void deleteByIdRoleIdAndIdPermissionId(UUID roleId, UUID permissionId);
}
