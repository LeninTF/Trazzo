package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.util.List;
import java.util.Optional;

public interface UserBiometriaRepositoryPort {
    UserBiometria save(UserBiometria userBiometria);
    Optional<UserBiometria> findById(Long id);
    List<UserBiometria> findAll(Long tenantUserId, Long deviceId, Boolean activo, int page, int size);
    long count(Long tenantUserId, Long deviceId, Boolean activo);
    Optional<UserBiometria> findByTenantUserIdAndFingerIndex(Long tenantUserId, Integer fingerIndex);
    List<UserBiometria> findByTenantUserId(Long tenantUserId);
}
