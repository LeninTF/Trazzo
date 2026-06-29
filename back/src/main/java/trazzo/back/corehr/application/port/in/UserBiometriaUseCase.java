package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;

public interface UserBiometriaUseCase {
    PaginatedResult<UserBiometriaResult> findAll(Long tenantUserId, Long deviceId, Boolean activo, int page, int size);
    UserBiometriaResult patchActivo(Long id, boolean activo);
}
