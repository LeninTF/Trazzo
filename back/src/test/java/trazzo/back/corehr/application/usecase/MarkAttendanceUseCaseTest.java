package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.port.out.AttendanceNotificationPort;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.TransportPayload;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

class MarkAttendanceUseCaseTest {

    private static final byte[] PROBE_TEMPLATE = {7, 7, 7};

    private DeviceRepositoryPort deviceRepository;
    private BiometricCryptoPort biometricCrypto;
    private BiometricMatchingPort biometricMatching;
    private UserBiometriaRepositoryPort biometriaRepository;
    private UserScheduleRepositoryPort userScheduleRepository;
    private AttendanceRepositoryPort attendanceRepository;
    private EventPublisherPort eventPublisher;
    private AttendanceNotificationPort notificationPort;
    private MarkAttendanceUseCase useCase;

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepositoryPort.class);
        biometricCrypto = mock(BiometricCryptoPort.class);
        biometricMatching = mock(BiometricMatchingPort.class);
        biometriaRepository = mock(UserBiometriaRepositoryPort.class);
        userScheduleRepository = mock(UserScheduleRepositoryPort.class);
        attendanceRepository = mock(AttendanceRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        notificationPort = mock(AttendanceNotificationPort.class);
        useCase = new MarkAttendanceUseCase(
                deviceRepository, biometricCrypto, biometricMatching,
                biometriaRepository, userScheduleRepository,
                attendanceRepository, eventPublisher, notificationPort
        );
        ReflectionTestUtils.setField(useCase, "matchThreshold", 40);
        when(biometricCrypto.openTransport(any())).thenReturn(PROBE_TEMPLATE);
        when(biometriaRepository.findActiveAfterId(0L, 1000)).thenReturn(List.of(enrolled(1L, 1L)));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<Attendance>getArgument(0));
    }

    private static UserBiometria enrolled(long id, long tenantUserId) {
        var now = LocalDateTime.now();
        return UserBiometria.restore(id, tenantUserId, 1L, "DEV-01", 1, "c2VsbGFkbw==", null, "aXY=", "dGFn",
                now, true, now, now, "storage-v1");
    }

    private static final List<UserBiometria> FIRST_PAGE =
            LongStream.rangeClosed(1, 1000).mapToObj(id -> enrolled(id, id)).toList();
    private static final List<UserBiometria> SECOND_PAGE = List.of(enrolled(1001L, 77L));

    private void twoPagesOfTemplates() {
        when(biometriaRepository.findActiveAfterId(0L, 1000)).thenReturn(FIRST_PAGE);
        when(biometriaRepository.findActiveAfterId(1000L, 1000)).thenReturn(SECOND_PAGE);
    }

    @Test
    void mark_identifiesSomeoneEnrolledBeyondTheFirstThousandTemplates() {
        activeDevice(1L);
        twoPagesOfTemplates();
        when(biometricMatching.identify(PROBE_TEMPLATE, FIRST_PAGE, 40)).thenReturn(Optional.empty());
        when(biometricMatching.identify(PROBE_TEMPLATE, SECOND_PAGE, 40))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(77L, 90)));

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0)));

        assertEquals(77L, result.tenantUserId());
        verify(biometriaRepository, never()).findActiveAfterId(eq(1001L), anyInt());
    }

    @Test
    void mark_aBetterMatchOnALaterPageWins() {
        activeDevice(1L);
        twoPagesOfTemplates();
        when(biometricMatching.identify(PROBE_TEMPLATE, FIRST_PAGE, 40))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(5L, 50)));
        when(biometricMatching.identify(PROBE_TEMPLATE, SECOND_PAGE, 40))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(77L, 90)));

        assertEquals(77L, useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))).tenantUserId());
    }

    @Test
    void mark_aBetterMatchOnAnEarlierPageIsKept() {
        activeDevice(1L);
        twoPagesOfTemplates();
        when(biometricMatching.identify(PROBE_TEMPLATE, FIRST_PAGE, 40))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(5L, 90)));
        when(biometricMatching.identify(PROBE_TEMPLATE, SECOND_PAGE, 40))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(77L, 50)));

        assertEquals(5L, useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))).tenantUserId());
    }

    private static MarkAttendanceCommand command(LocalDateTime capturedAt) {
        return new MarkAttendanceCommand("tpl", "key", "iv", "tag", capturedAt, "DEV-01");
    }

    private void activeDevice(long id) {
        when(deviceRepository.findByCode("DEV-01")).thenReturn(Optional.of(
                Device.restore(id, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
    }

    private void identifiedAs(long tenantUserId) {
        when(biometricMatching.identify(eq(PROBE_TEMPLATE), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(tenantUserId, 85)));
    }

    private void scheduleAt(LocalTime entry) {
        var userSchedule = UserSchedule.restore(1L, 1L, 10L, "Turno mañana", entry, LocalTime.of(17, 0),
                LocalDateTime.now(), LocalDateTime.now());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of(userSchedule));
    }

    @Test
    void mark_passesTheTransportFieldsUnchangedToTheCryptoPort() {
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(eq(1L), any())).thenReturn(Optional.empty());

        useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0)));

        ArgumentCaptor<TransportPayload> payload = ArgumentCaptor.forClass(TransportPayload.class);
        verify(biometricCrypto).openTransport(payload.capture());
        assertEquals(new TransportPayload("tpl", "key", "iv", "tag"), payload.getValue());
    }

    @Test
    void mark_shouldPropagateDecryptionErrorsAsBadRequest() {
        activeDevice(1L);
        when(biometricCrypto.openTransport(any()))
                .thenThrow(new BiometricDecryptionException("Error descifrado biométrico: Padding error", null));

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))));
        verifyNoInteractions(attendanceRepository, biometricMatching);
    }

    @Test
    void mark_shouldReturnCheckIn_whenFirstMarking() {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 0);
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate())).thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());

        var result = useCase.mark(command(capturedAt));

        assertEquals(1L, result.tenantUserId());
        assertEquals(capturedAt.toLocalDate(), result.attendanceDate());
        assertEquals(AttendanceState.PUNTUAL, result.state());
        verify(attendanceRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void mark_shouldRegisterCheckOut_whenCheckInExists() {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 18, 0);
        var checkInTime = LocalDateTime.of(2025, 7, 15, 9, 0);
        var attendanceDate = LocalDate.of(2025, 7, 15);
        activeDevice(1L);
        identifiedAs(1L);
        var existing = Attendance.restore("att-1", 1L, 10L, 100L, checkInTime, null, attendanceDate, 0,
                AttendanceState.PUNTUAL, null, null, checkInTime, checkInTime);
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate)).thenReturn(Optional.of(existing));

        var result = useCase.mark(command(capturedAt));

        assertEquals(capturedAt, result.checkOut());
        assertEquals(checkInTime, result.checkIn());
        verify(attendanceRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void mark_shouldThrow_whenDeviceNotFound() {
        when(deviceRepository.findByCode("DEV-01")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))));
        verifyNoInteractions(biometricCrypto);
    }

    @Test
    void mark_shouldThrow_whenDeviceInactive() {
        when(deviceRepository.findByCode("DEV-01")).thenReturn(Optional.of(
                Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, false, LocalDateTime.now())));

        assertThrows(IllegalStateException.class, () -> useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))));
        verifyNoInteractions(biometricCrypto);
    }

    @Test
    void mark_shouldThrow_whenNoBiometricMatch() {
        activeDevice(1L);
        when(biometricMatching.identify(any(), anyList(), eq(40))).thenReturn(Optional.of(BiometricIdentifyResult.noMatch()));

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))));
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void mark_shouldThrow_whenBiometricIdentifyReturnsEmpty() {
        activeDevice(1L);
        when(biometricMatching.identify(any(), anyList(), eq(40))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0))));
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void mark_shouldThrow_whenBothCheckInAndCheckOutExist() {
        var attendanceDate = LocalDate.of(2025, 7, 15);
        activeDevice(1L);
        identifiedAs(1L);
        var complete = Attendance.restore("att-1", 1L, 10L, 100L,
                LocalDateTime.of(2025, 7, 15, 9, 0), LocalDateTime.of(2025, 7, 15, 17, 0), attendanceDate, 0,
                AttendanceState.PUNTUAL, null, null, LocalDateTime.now(), LocalDateTime.now());
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate)).thenReturn(Optional.of(complete));

        assertThrows(IllegalStateException.class, () -> useCase.mark(command(LocalDateTime.of(2025, 7, 15, 19, 0))));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void mark_shouldNotifyTardanza_whenStateIsTardanza() {
        var attendanceDate = LocalDate.of(2025, 7, 15);
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate)).thenReturn(Optional.empty());
        scheduleAt(LocalTime.of(8, 0));

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 30)));

        assertEquals(AttendanceState.TARDANZA, result.state());
        assertEquals(90, result.minutesLate());
        verify(notificationPort).notifyTardanza(isNull(), isNull(), eq(90), eq(attendanceDate));
    }

    @Test
    void mark_shouldNotFail_whenNotificationFails() {
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(eq(1L), any())).thenReturn(Optional.empty());
        scheduleAt(LocalTime.of(8, 0));
        doThrow(new RuntimeException("smtp down")).when(notificationPort).notifyTardanza(any(), any(), anyInt(), any());

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 30)));

        assertEquals(AttendanceState.TARDANZA, result.state());
        verify(notificationPort).notifyTardanza(isNull(), isNull(), anyInt(), any());
    }

    @Test
    void mark_shouldUseScheduleEntryTime_whenScheduleExists() {
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(eq(1L), any())).thenReturn(Optional.empty());
        scheduleAt(LocalTime.of(8, 0));

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0)));

        assertEquals(10L, result.scheduleId());
        assertEquals(AttendanceState.TARDANZA, result.state());
        assertEquals(60, result.minutesLate());
    }

    @Test
    void mark_shouldReturnPuntual_whenCheckingInBeforeScheduleEntry() {
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(eq(1L), any())).thenReturn(Optional.empty());
        scheduleAt(LocalTime.of(8, 0));

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 7, 55)));

        assertEquals(AttendanceState.PUNTUAL, result.state());
        assertEquals(0, result.minutesLate());
        verifyNoInteractions(notificationPort);
    }

    @Test
    void mark_shouldReturnPuntual_whenNoSchedule() {
        activeDevice(1L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(eq(1L), any())).thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0)));

        assertEquals(AttendanceState.PUNTUAL, result.state());
        assertNull(result.scheduleId());
    }

    @Test
    void mark_shouldReturnCorrectDeviceId_whenDeviceExists() {
        activeDevice(42L);
        identifiedAs(1L);
        when(attendanceRepository.findByTenantUserIdAndDate(eq(1L), any())).thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());

        var result = useCase.mark(command(LocalDateTime.of(2025, 7, 15, 9, 0)));

        assertEquals(42L, result.deviceId());
    }
}
