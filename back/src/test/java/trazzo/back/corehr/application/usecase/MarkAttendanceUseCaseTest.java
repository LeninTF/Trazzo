package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import trazzo.back.corehr.application.dto.command.MarkAttendanceCommand;
import trazzo.back.corehr.application.port.out.AttendanceNotificationPort;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.BiometricIdentifyResult;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;

import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

class MarkAttendanceUseCaseTest {

    private DeviceRepositoryPort deviceRepository;
    private CryptoKeyProviderPort cryptoKeyProvider;
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
        cryptoKeyProvider = mock(CryptoKeyProviderPort.class);
        biometricMatching = mock(BiometricMatchingPort.class);
        biometriaRepository = mock(UserBiometriaRepositoryPort.class);
        userScheduleRepository = mock(UserScheduleRepositoryPort.class);
        attendanceRepository = mock(AttendanceRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        notificationPort = mock(AttendanceNotificationPort.class);
        useCase = new MarkAttendanceUseCase(
                deviceRepository, cryptoKeyProvider, biometricMatching,
                biometriaRepository, userScheduleRepository,
                attendanceRepository, eventPublisher, notificationPort
        );
        ReflectionTestUtils.setField(useCase, "matchThreshold", 40);
    }

    private void stubDecryptAndIdentify(Long tenantUserId) throws Exception {
        var privateKey = mock(PrivateKey.class);
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privateKey);

        javax.crypto.Cipher rsaCipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, privateKey);
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        var pubKey = pair.getPublic();
        var privKey = pair.getPrivate();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(privKey);

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] templateBytes = "fake-biometric-template".getBytes();
        byte[] encryptedTemplate = aesEnc.doFinal(templateBytes);
        byte[] encTag = java.util.Arrays.copyOfRange(encryptedTemplate, encryptedTemplate.length - 16, encryptedTemplate.length);
        byte[] encBody = java.util.Arrays.copyOfRange(encryptedTemplate, 0, encryptedTemplate.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pubKey);
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        String encTemplateB64 = java.util.Base64.getEncoder().encodeToString(encBody);
        String encAesKeyB64 = java.util.Base64.getEncoder().encodeToString(encryptedAesKey);
        String ivB64 = java.util.Base64.getEncoder().encodeToString(iv);
        String tagB64 = java.util.Base64.getEncoder().encodeToString(encTag);

        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(tenantUserId, 85)));
    }

    private MarkAttendanceCommand buildCommand(String deviceCode) {
        return new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString("template".getBytes()),
                java.util.Base64.getEncoder().encodeToString("key".getBytes()),
                java.util.Base64.getEncoder().encodeToString("iv".getBytes()),
                java.util.Base64.getEncoder().encodeToString("tag".getBytes()),
                LocalDateTime.of(2025, 7, 15, 9, 0),
                deviceCode
        );
    }

    @Test
    void mark_shouldReturnCheckIn_whenFirstMarking() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 0);
        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString("tpl".getBytes()),
                java.util.Base64.getEncoder().encodeToString("key".getBytes()),
                java.util.Base64.getEncoder().encodeToString("iv".getBytes()),
                java.util.Base64.getEncoder().encodeToString("tag".getBytes()),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));

        var privateKey = mock(PrivateKey.class);
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var realCommand = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate()))
                .thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(realCommand);

        assertNotNull(result);
        assertEquals(1L, result.tenantUserId());
        assertEquals(capturedAt.toLocalDate(), result.attendanceDate());
        assertEquals(AttendanceState.PUNTUAL, result.state());
        verify(attendanceRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void mark_shouldRegisterCheckOut_whenCheckInExists() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 18, 0);
        var checkInTime = LocalDateTime.of(2025, 7, 15, 9, 0);
        var attendanceDate = LocalDate.of(2025, 7, 15);

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString("tpl".getBytes()),
                java.util.Base64.getEncoder().encodeToString("key".getBytes()),
                java.util.Base64.getEncoder().encodeToString("iv".getBytes()),
                java.util.Base64.getEncoder().encodeToString("tag".getBytes()),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var realCommand = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));

        var existingAttendance = trazzo.back.corehr.domain.model.attendance.Attendance.restore(
                "att-1", 1L, 10L, 100L, checkInTime, null, attendanceDate, 0,
                AttendanceState.PUNTUAL, null, null, checkInTime, checkInTime);
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate))
                .thenReturn(Optional.of(existingAttendance));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(realCommand);

        assertNotNull(result);
        assertEquals(capturedAt, result.checkOut());
        assertEquals(checkInTime, result.checkIn());
        verify(attendanceRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void mark_shouldThrow_whenDeviceNotFound() {
        var command = buildCommand("DEV-999");
        when(deviceRepository.findByCode("DEV-999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(command));
        verifyNoInteractions(biometricMatching);
    }

    @Test
    void mark_shouldThrow_whenDeviceInactive() {
        var command = buildCommand("DEV-01");
        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, false, LocalDateTime.now())));

        assertThrows(IllegalStateException.class, () -> useCase.mark(command));
        verifyNoInteractions(biometricMatching);
    }

    @Test
    void mark_shouldThrow_whenNoBiometricMatch() throws Exception {
        var command = buildCommand("DEV-01");
        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var realCommand = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                LocalDateTime.of(2025, 7, 15, 9, 0), "DEV-01"
        );

        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.noMatch()));

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(realCommand));
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void mark_shouldThrow_whenBiometricIdentifyReturnsEmpty() throws Exception {
        var command = buildCommand("DEV-01");
        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var realCommand = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                LocalDateTime.of(2025, 7, 15, 9, 0), "DEV-01"
        );

        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.mark(realCommand));
    }

    @Test
    void mark_shouldThrow_whenBothCheckInAndCheckOutExist() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 10, 0);
        var command = buildCommand("DEV-01");
        var attendanceDate = LocalDate.of(2025, 7, 15);

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var realCommand = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));

        var existingAttendance = trazzo.back.corehr.domain.model.attendance.Attendance.restore(
                "att-1", 1L, 10L, 100L,
                LocalDateTime.of(2025, 7, 15, 9, 0),
                LocalDateTime.of(2025, 7, 15, 18, 0),
                attendanceDate, 0, AttendanceState.PUNTUAL, null, null,
                LocalDateTime.of(2025, 7, 15, 9, 0),
                LocalDateTime.of(2025, 7, 15, 18, 0));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate))
                .thenReturn(Optional.of(existingAttendance));

        assertThrows(IllegalStateException.class, () -> useCase.mark(realCommand));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void mark_shouldNotifyTardanza_whenStateIsTardanza() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 30);
        var attendanceDate = LocalDate.of(2025, 7, 15);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate))
                .thenReturn(Optional.empty());

        var userSchedule = UserSchedule.restore(
                1L, 1L, 10L, "Turno mañana",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalDateTime.now(), LocalDateTime.now());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of(userSchedule));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(command);

        assertEquals(AttendanceState.TARDANZA, result.state());
        assertEquals(90, result.minutesLate());
        verify(notificationPort).notifyTardanza(isNull(), isNull(), eq(90), eq(attendanceDate));
    }

    @Test
    void mark_shouldNotNotify_whenNotificationFails() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 30);
        var attendanceDate = LocalDate.of(2025, 7, 15);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, attendanceDate))
                .thenReturn(Optional.empty());

        var userSchedule = UserSchedule.restore(
                1L, 1L, 10L, "Turno mañana",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalDateTime.now(), LocalDateTime.now());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of(userSchedule));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));
        doThrow(new RuntimeException("Email down")).when(notificationPort)
                .notifyTardanza(isNull(), isNull(), anyInt(), any());

        var result = useCase.mark(command);

        assertNotNull(result);
        assertEquals(AttendanceState.TARDANZA, result.state());
        verify(notificationPort).notifyTardanza(isNull(), isNull(), anyInt(), any());
    }

    @Test
    void mark_shouldUseScheduleEntryTime_whenScheduleExists() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 0);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate()))
                .thenReturn(Optional.empty());

        var userSchedule = UserSchedule.restore(
                1L, 1L, 10L, "Turno mañana",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalDateTime.now(), LocalDateTime.now());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of(userSchedule));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(command);

        assertNotNull(result);
        assertEquals(10L, result.scheduleId());
        assertEquals(AttendanceState.TARDANZA, result.state());
        assertEquals(60, result.minutesLate());
        verify(notificationPort).notifyTardanza(isNull(), isNull(), eq(60), any());
    }

    @Test
    void mark_shouldReturnPuntual_whenCheckingInBeforeScheduleEntry() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 7, 55);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate()))
                .thenReturn(Optional.empty());

        var userSchedule = UserSchedule.restore(
                1L, 1L, 10L, "Turno mañana",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalDateTime.now(), LocalDateTime.now());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of(userSchedule));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(command);

        assertEquals(AttendanceState.PUNTUAL, result.state());
        assertEquals(0, result.minutesLate());
        verifyNoInteractions(notificationPort);
    }

    @Test
    void mark_shouldReturnPuntual_whenNoSchedule() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 0);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate()))
                .thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(command);

        assertEquals(AttendanceState.PUNTUAL, result.state());
        assertEquals(0, result.minutesLate());
        assertNull(result.scheduleId());
        verifyNoInteractions(notificationPort);
    }

    @Test
    void mark_shouldPublishDomainEvents_afterSaving() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 0);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(1L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate()))
                .thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        useCase.mark(command);

        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void mark_shouldReturnCorrectDeviceId_whenDeviceExists() throws Exception {
        var capturedAt = LocalDateTime.of(2025, 7, 15, 9, 0);

        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var pair = kpg.generateKeyPair();
        when(cryptoKeyProvider.getPrivateKey()).thenReturn(pair.getPrivate());

        javax.crypto.SecretKey aesKey = javax.crypto.KeyGenerator.getInstance("AES").generateKey();
        javax.crypto.Cipher aesEnc = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        aesEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
        aesEnc.updateAAD("biometric-identify".getBytes());
        byte[] combined = aesEnc.doFinal("template".getBytes());
        byte[] encTag = java.util.Arrays.copyOfRange(combined, combined.length - 16, combined.length);
        byte[] encBody = java.util.Arrays.copyOfRange(combined, 0, combined.length - 16);

        javax.crypto.Cipher rsaEnc = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaEnc.init(javax.crypto.Cipher.ENCRYPT_MODE, pair.getPublic());
        byte[] encryptedAesKey = rsaEnc.doFinal(aesKey.getEncoded());

        var command = new MarkAttendanceCommand(
                java.util.Base64.getEncoder().encodeToString(encBody),
                java.util.Base64.getEncoder().encodeToString(encryptedAesKey),
                java.util.Base64.getEncoder().encodeToString(iv),
                java.util.Base64.getEncoder().encodeToString(encTag),
                capturedAt, "DEV-01"
        );

        when(deviceRepository.findByCode("DEV-01"))
                .thenReturn(Optional.of(Device.restore(42L, "DEV-01", "Sensor A", "192.168.1.1", 8080, "Sede Central", 10L, true, LocalDateTime.now())));
        when(biometriaRepository.findAll(isNull(), isNull(), eq(true), eq(0), eq(10000)))
                .thenReturn(List.of());
        when(biometricMatching.identify(any(byte[].class), anyList(), eq(40)))
                .thenReturn(Optional.of(BiometricIdentifyResult.match(1L, 85)));
        when(attendanceRepository.findByTenantUserIdAndDate(1L, capturedAt.toLocalDate()))
                .thenReturn(Optional.empty());
        when(userScheduleRepository.findByTenantUserId(1L)).thenReturn(List.of());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.<trazzo.back.corehr.domain.model.attendance.Attendance>getArgument(0));

        var result = useCase.mark(command);

        assertEquals(42L, result.deviceId());
    }
}
