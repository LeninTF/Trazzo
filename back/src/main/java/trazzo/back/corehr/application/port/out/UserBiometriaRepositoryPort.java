package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.util.List;
import java.util.Optional;

public interface UserBiometriaRepositoryPort {
    UserBiometria save(UserBiometria userBiometria);
    Optional<UserBiometria> findById(Long id);
    List<UserBiometria> findAll(Long tenantUserId, Long deviceId, Boolean activo, int page, int size);
    long count(Long tenantUserId, Long deviceId, Boolean activo);
    List<UserBiometria> findActiveByTenantUserIdAndFingerIndex(Long tenantUserId, Integer fingerIndex);
    /** Hasta {@code limit} filas activas con id mayor que {@code afterId}, en orden de id. */
    List<UserBiometria> findActiveAfterId(long afterId, int limit);
    List<UserBiometria> findByTenantUserId(Long tenantUserId);
}
