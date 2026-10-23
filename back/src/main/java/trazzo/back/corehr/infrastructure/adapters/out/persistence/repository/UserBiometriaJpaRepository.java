package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserBiometriaEntity;

import java.util.List;

@Repository
public interface UserBiometriaJpaRepository extends JpaRepository<UserBiometriaEntity, Long> {

    @Query("SELECT u FROM UserBiometriaEntity u WHERE " +
           "(:tenantUserId IS NULL OR u.tenantUserId = :tenantUserId) AND " +
           "(:deviceId IS NULL OR u.deviceId = :deviceId) AND " +
           "(:activo IS NULL OR u.activo = :activo)")
    Page<UserBiometriaEntity> findByTenantUserIdAndDeviceIdAndActivo(
            @Param("tenantUserId") Long tenantUserId,
            @Param("deviceId") Long deviceId,
            @Param("activo") Boolean activo,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserBiometriaEntity u WHERE " +
           "(:tenantUserId IS NULL OR u.tenantUserId = :tenantUserId) AND " +
           "(:deviceId IS NULL OR u.deviceId = :deviceId) AND " +
           "(:activo IS NULL OR u.activo = :activo)")
    long countByTenantUserIdAndDeviceIdAndActivo(
            @Param("tenantUserId") Long tenantUserId,
            @Param("deviceId") Long deviceId,
            @Param("activo") Boolean activo);

    List<UserBiometriaEntity> findByTenantUserIdAndFingerIndexAndActivoTrue(Long tenantUserId, Integer fingerIndex);

    // Keyset (id > último visto): a diferencia de OFFSET, no salta filas si otra se desactiva entre páginas.
    @Query("SELECT u FROM UserBiometriaEntity u WHERE u.activo = true AND u.id > :afterId ORDER BY u.id")
    List<UserBiometriaEntity> findActiveAfterId(@Param("afterId") Long afterId, Pageable pageable);

    List<UserBiometriaEntity> findByTenantUserId(Long tenantUserId);
}
