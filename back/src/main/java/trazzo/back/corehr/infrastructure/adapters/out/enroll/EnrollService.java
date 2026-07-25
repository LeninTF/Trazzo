package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EnrollService {

    private final TenantUserPort tenantUserPort;
    private final DeviceRepositoryPort deviceRepository;
    private final UserBiometriaRepositoryPort userBiometriaRepository;
    private final EnrollSessionStore enrollSessionStore;

    public EnrollSessionResponse initEnroll(Long tenantUserId, Long deviceId, Integer fingerIndex) {
        if (!tenantUserPort.existsById(tenantUserId)) {
            throw new IllegalArgumentException("TenantUser no encontrado: " + tenantUserId);
        }

        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + deviceId));

        if (!device.isState()) {
            throw new IllegalStateException("El dispositivo no está activo");
        }

        if (enrollSessionStore.existsActiveSession(tenantUserId, deviceId)) {
            throw new IllegalStateException("Ya existe una sesión de enrolamiento activa para este usuario y dispositivo");
        }

        var enrollToken = UUID.randomUUID().toString();
        var expiresAt = LocalDateTime.now().plusSeconds(120);

        var session = new EnrollSession(enrollToken, tenantUserId, deviceId, fingerIndex, device.getCode(), expiresAt);
        enrollSessionStore.createSession(session);

        return new EnrollSessionResponse(enrollToken, tenantUserId, deviceId, fingerIndex, device.getCode(), expiresAt);
    }

    public Optional<EnrollSession> findPendingSession(String deviceCode) {
        return enrollSessionStore.findPendingByDeviceCode(deviceCode);
    }

    public UserBiometriaResult completeEnroll(
            String enrollToken,
            String deviceCode,
            Integer fingerIndex,
            String encryptedTemplateBase64,
            String encryptedAesKeyBase64,
            String ivBase64,
            String tagBase64,
            LocalDateTime capturedAtUtc
    ) {
        var session = enrollSessionStore.findAndConsume(enrollToken);
        if (session == null) {
            throw new IllegalArgumentException("Token de enrolamiento inválido o expirado");
        }

        if (!session.fingerIndex().equals(fingerIndex)) {
            throw new IllegalArgumentException("El índice de huella no coincide con la sesión");
        }

        if (!session.deviceCode().equals(deviceCode)) {
            throw new IllegalArgumentException("El código de dispositivo no coincide con la sesión");
        }

        userBiometriaRepository.findByTenantUserIdAndFingerIndex(session.tenantUserId(), fingerIndex)
                .ifPresent(existing -> {
                    existing.deactivate();
                    userBiometriaRepository.save(existing);
                });

        var biometria = UserBiometria.create(
                session.tenantUserId(),
                session.deviceId(),
                deviceCode,
                fingerIndex,
                encryptedTemplateBase64,
                encryptedAesKeyBase64,
                ivBase64,
                tagBase64,
                capturedAtUtc
        );

        var saved = userBiometriaRepository.save(biometria);
        return toResult(saved);
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
