package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.port.out.AttendanceNotificationPort;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.TransportPayload;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.application.port.out.BusinessTimeZonePort;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class MarkAttendanceUseCase {

    static final int MATCH_PAGE_SIZE = 1000;

    private final DeviceRepositoryPort deviceRepository;
    private final BiometricCryptoPort biometricCrypto;
    private final BiometricMatchingPort biometricMatching;
    private final UserBiometriaRepositoryPort biometriaRepository;
    private final UserScheduleRepositoryPort userScheduleRepository;
    private final ToleranciaRepositoryPort toleranciaRepository;
    private final AttendanceRepositoryPort attendanceRepository;
    private final EventPublisherPort eventPublisher;
    private final AttendanceNotificationPort notificationPort;
    private final BusinessTimeZonePort businessTimeZone;

    @Value("${corehr.biometric.match-threshold:40}")
    private int matchThreshold;

    public AttendanceResult mark(MarkAttendanceCommand command) {
        var device = deviceRepository.findByCode(command.deviceCode())
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + command.deviceCode()));

        if (!device.isState()) {
            throw new IllegalStateException("El dispositivo está inactivo: " + command.deviceCode());
        }

        byte[] decryptedTemplate = biometricCrypto.openTransport(new TransportPayload(
                command.encryptedTemplateBase64(),
                command.encryptedAesKeyBase64(),
                command.ivBase64(),
                command.tagBase64()));

        var matchResult = identifyAmongActiveTemplates(decryptedTemplate)
                .orElseThrow(() -> new IllegalArgumentException("Huella no reconocida"));

        if (!matchResult.matched()) {
            throw new IllegalArgumentException("Huella no reconocida");
        }

        Long tenantUserId = matchResult.tenantUserId();
        // El agente envía el instante en UTC; los horarios (y el día) son locales del negocio.
        LocalDateTime capturedAt = command.capturedAtUtc().atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(businessTimeZone.zone())
                .toLocalDateTime();
        LocalDate attendanceDate = capturedAt.toLocalDate();

        var existingAttendance = attendanceRepository.findByTenantUserIdAndDate(tenantUserId, attendanceDate);

        if (existingAttendance.isPresent()) {
            var existing = existingAttendance.get();
            // La misma captura reenviada (el agente la encola si la marcación en vivo falla, también por timeout
            // aunque el backend ya la hubiera registrado): no es una salida nueva.
            if (capturedAt.equals(existing.getCheckIn()) || capturedAt.equals(existing.getCheckOut())) {
                return toResult(existing);
            }
            if (existing.getCheckIn() != null && existing.getCheckOut() == null) {
                existing.registerCheckOutAt(capturedAt);
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
                entryTime, toleranceMinutes, capturedAt, command.offlineEventId(), command.deviceCode()
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

    // Recorre todas las huellas activas por páginas (sin tope) y se queda con la de mayor confianza.
    private Optional<BiometricIdentifyResult> identifyAmongActiveTemplates(byte[] probe) {
        Optional<BiometricIdentifyResult> best = Optional.empty();
        long afterId = 0;
        while (true) {
            List<UserBiometria> page = biometriaRepository.findActiveAfterId(afterId, MATCH_PAGE_SIZE);
            if (page.isEmpty()) {
                return best;
            }
            var candidate = biometricMatching.identify(probe, page, matchThreshold);
            if (candidate.isPresent() && (best.isEmpty() || candidate.get().confidence() > best.get().confidence())) {
                best = candidate;
            }
            if (page.size() < MATCH_PAGE_SIZE) {
                return best;
            }
            afterId = page.getLast().getId();
        }
    }

    private record ScheduleInfo(Long scheduleId, LocalTime entryTime, int toleranceMinutes) {}

    private Optional<ScheduleInfo> resolveSchedule(Long tenantUserId) {
        var userSchedules = userScheduleRepository.findByTenantUserId(tenantUserId);
        if (userSchedules.isEmpty()) {
            return Optional.empty();
        }
        var userSchedule = userSchedules.getFirst();
        int tolerance = toleranciaRepository.findActiveByScheduleIdAndType(userSchedule.getScheduleId(), ToleranciaType.ENTRADA)
                .map(Tolerancia::getMinutes)
                .orElse(0);
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
