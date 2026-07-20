package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSession;

import java.util.Optional;

public interface UserBiometriaUseCase {
    PaginatedResult<UserBiometriaResult> findAll(Long tenantUserId, Long deviceId, Boolean activo, int page, int size);
    UserBiometriaResult patchActivo(Long id, boolean activo);
    Optional<EnrollSession> findPendingEnrollSession(String deviceCode);
}
