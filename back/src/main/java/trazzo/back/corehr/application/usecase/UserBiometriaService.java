package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;
import trazzo.back.corehr.application.port.in.UserBiometriaUseCase;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSession;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollService;

import java.util.Optional;

@RequiredArgsConstructor
public class UserBiometriaService implements UserBiometriaUseCase {

    private final UserBiometriaRepositoryPort userBiometriaRepository;
    private final EnrollService enrollService;

    @Override
    public PaginatedResult<UserBiometriaResult> findAll(Long tenantUserId, Long deviceId, Boolean activo, int page, int size) {
        var items = userBiometriaRepository.findAll(tenantUserId, deviceId, activo, page, size);
        var total = userBiometriaRepository.count(tenantUserId, deviceId, activo);
        var results = items.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public UserBiometriaResult patchActivo(Long id, boolean activo) {
        var biometria = userBiometriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro biométrico no encontrado: " + id));
        if (activo) {
            biometria.activate();
        } else {
            biometria.deactivate();
        }
        var saved = userBiometriaRepository.save(biometria);
        return toResult(saved);
    }

    @Override
    public Optional<EnrollSession> findPendingEnrollSession(String deviceCode) {
        return enrollService.findPendingSession(deviceCode);
    }

    private UserBiometriaResult toResult(UserBiometria ub) {
        return new UserBiometriaResult(
                ub.getId(),
                ub.getTenantUserId(),
                ub.getDeviceId(),
                ub.getDeviceCode(),
                ub.getFingerIndex(),
                ub.isActivo(),
                ub.getCapturadoEn(),
                ub.getCreatedAt(),
                ub.getUpdatedAt()
        );
    }
}
