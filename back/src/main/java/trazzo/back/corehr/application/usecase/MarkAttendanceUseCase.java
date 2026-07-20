package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.port.out.AttendanceNotificationPort;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class MarkAttendanceUseCase {

    private final DeviceRepositoryPort deviceRepository;
    private final CryptoKeyProviderPort cryptoKeyProvider;
    private final BiometricMatchingPort biometricMatching;
    private final UserBiometriaRepositoryPort biometriaRepository;
    private final UserScheduleRepositoryPort userScheduleRepository;
    private final AttendanceRepositoryPort attendanceRepository;
    private final EventPublisherPort eventPublisher;
    private final AttendanceNotificationPort notificationPort;

    @Value("${corehr.biometric.match-threshold:40}")
    private int matchThreshold;

    public AttendanceResult mark(MarkAttendanceCommand command) {
        var device = deviceRepository.findByCode(command.deviceCode())
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + command.deviceCode()));

        if (!device.isState()) {
            throw new IllegalStateException("El dispositivo está inactivo: " + command.deviceCode());
        }

        byte[] decryptedTemplate = decryptTemplate(command);

        var allTemplates = biometriaRepository.findAll(null, null, true, 0, 10000);

        var matchResult = biometricMatching.identify(decryptedTemplate, allTemplates, matchThreshold)
                .orElseThrow(() -> new IllegalArgumentException("Huella no reconocida"));

        if (!matchResult.matched()) {
            throw new IllegalArgumentException("Huella no reconocida");
        }

        Long tenantUserId = matchResult.tenantUserId();
        LocalDate attendanceDate = command.capturedAtUtc().toLocalDate();

        var existingAttendance = attendanceRepository.findByTenantUserIdAndDate(tenantUserId, attendanceDate);

        if (existingAttendance.isPresent()) {
            var existing = existingAttendance.get();
            if (existing.getCheckIn() != null && existing.getCheckOut() == null) {
                existing.registerCheckOutAt(command.capturedAtUtc());
                var saved = attendanceRepository.save(existing);
                var events = saved.pullDomainEvents();
                events.forEach(eventPublisher::publish);
                return toResult(saved);
            }
            throw new IllegalStateException("Ya marcó entrada y salida para esta fecha: " + attendanceDate);
        }

        var scheduleInfo = resolveSchedule(tenantUserId);
        Long scheduleId = scheduleInfo.map(ScheduleInfo::scheduleId).orElse(null);
        LocalTime entryTime = scheduleInfo.map(ScheduleInfo::entryTime).orElse(null);
        int toleranceMinutes = scheduleInfo.map(ScheduleInfo::toleranceMinutes).orElse(0);

        var attendance = Attendance.registerCheckInAt(
                tenantUserId, scheduleId, device.getId(),
                entryTime, toleranceMinutes, command.capturedAtUtc()
        );

        var saved = attendanceRepository.save(attendance);
        var events = saved.pullDomainEvents();
        events.forEach(eventPublisher::publish);

        if (saved.getState() == AttendanceState.TARDANZA) {
            try {
                notificationPort.notifyTardanza(null, null, saved.getMinutesLate(), saved.getAttendanceDate());
            } catch (Exception e) {
                log.warn("Failed to send tardanza notification: {}", e.getMessage());
            }
        }

        return toResult(saved);
    }

    private byte[] decryptTemplate(MarkAttendanceCommand command) {
        try {
            PrivateKey privateKey = cryptoKeyProvider.getPrivateKey();
            byte[] encryptedAesKey = Base64.getDecoder().decode(command.encryptedAesKeyBase64());

            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

            byte[] iv = Base64.getDecoder().decode(command.ivBase64());
            byte[] tag = Base64.getDecoder().decode(command.tagBase64());
            byte[] encryptedTemplate = Base64.getDecoder().decode(command.encryptedTemplateBase64());

            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

            byte[] aad = "biometric-identify".getBytes();
            aesCipher.updateAAD(aad);

            byte[] combined = new byte[encryptedTemplate.length + tag.length];
            System.arraycopy(encryptedTemplate, 0, combined, 0, encryptedTemplate.length);
            System.arraycopy(tag, 0, combined, encryptedTemplate.length, tag.length);

            return aesCipher.doFinal(combined);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error descifrado biométrico: " + e.getMessage(), e);
        }
    }

    private record ScheduleInfo(Long scheduleId, LocalTime entryTime, int toleranceMinutes) {}

    private Optional<ScheduleInfo> resolveSchedule(Long tenantUserId) {
        var userSchedules = userScheduleRepository.findByTenantUserId(tenantUserId);
        if (userSchedules.isEmpty()) {
            return Optional.empty();
        }
        var userSchedule = userSchedules.getFirst();
        int tolerance = 0;
        return Optional.of(new ScheduleInfo(userSchedule.getScheduleId(), userSchedule.getEntryTime(), tolerance));
    }

    private AttendanceResult toResult(Attendance attendance) {
        return new AttendanceResult(
                attendance.getId(),
                attendance.getTenantUserId(),
                null,
                attendance.getScheduleId(),
                null,
                attendance.getDeviceId(),
                null,
                attendance.getCheckIn(),
                attendance.getCheckOut(),
                attendance.getAttendanceDate(),
                attendance.getMinutesLate(),
                attendance.getState(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}
