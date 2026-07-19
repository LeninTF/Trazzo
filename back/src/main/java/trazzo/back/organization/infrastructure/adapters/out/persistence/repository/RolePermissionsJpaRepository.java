package trazzo.back.organization.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RolePermissionsEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RolePermissionsId;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionsJpaRepository extends JpaRepository<RolePermissionsEntity, RolePermissionsId> {

    List<RolePermissionsEntity> findByIdRoleId(UUID roleId);

    boolean existsByIdRoleIdAndIdPermissionId(UUID roleId, UUID permissionId);

    // Unlike deleteById() (inherited from JpaRepository, self-transactional), a custom
    // derived delete query is implemented via a find-then-remove() strategy that requires
    // an active transaction at the call site — without this, it throws
    // TransactionRequiredException ("cannot reliably process 'remove' call").
    @Transactional
    void deleteByIdRoleIdAndIdPermissionId(UUID roleId, UUID permissionId);
}
