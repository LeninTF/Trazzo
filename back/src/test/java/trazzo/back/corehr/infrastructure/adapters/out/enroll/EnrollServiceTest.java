package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.StoredTemplate;
import trazzo.back.corehr.application.port.out.BiometricCryptoPort.TransportPayload;
import trazzo.back.corehr.application.port.out.BiometricMatchingPort;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.domain.exception.BiometricDecryptionException;
import trazzo.back.corehr.domain.exception.BiometricTemplateFormatException;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollServiceTest {

    private static final String TEMPLATE = "YmFzZTY0dGVtcGxhdGU=";
    private static final String AES_KEY = "YmFzZTY0YWVzS2V5";
    private static final String IV = "YmFzZTY0aXY=";
    private static final String TAG = "YmFzZTY0dGFn";

    @Mock
    private TenantUserPort tenantUserPort;

    @Mock
    private DeviceRepositoryPort deviceRepository;

    @Mock
    private UserBiometriaRepositoryPort userBiometriaRepository;

    @Mock
    private EnrollSessionStore enrollSessionStore;

    @Mock
    private BiometricCryptoPort biometricCrypto;

    @Mock
    private BiometricMatchingPort biometricMatching;

    private static final byte[] PLAIN_TEMPLATE = {9, 8, 7};
    private static final StoredTemplate SEALED = new StoredTemplate("c2VsbGFkbw==", "aXY=", "dGFn", "storage-v1");

    private void transportOpensAndSeals() {
        when(biometricCrypto.openTransport(new TransportPayload(TEMPLATE, AES_KEY, IV, TAG))).thenReturn(PLAIN_TEMPLATE);
        when(biometricCrypto.seal(PLAIN_TEMPLATE)).thenReturn(SEALED);
    }

    @InjectMocks
    private EnrollService enrollService;

    private final Long tenantUserId = 100L;
    private final Long deviceId = 200L;
    private final Integer fingerIndex = 1;

    private Device createActiveDevice() {
        return Device.restore(deviceId, "DEV-001", "Device 1", "192.168.1.1", 8080, "Office", 1L, true, LocalDateTime.now());
    }

    @Test
    void initEnroll_whenTenantUserNotFound_shouldThrow() {
        when(tenantUserPort.existsById(tenantUserId)).thenReturn(false);

        assertThatThrownBy(() -> enrollService.initEnroll(tenantUserId, deviceId, fingerIndex))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TenantUser no encontrado");
    }

    @Test
    void initEnroll_whenDeviceNotFound_shouldThrow() {
        when(tenantUserPort.existsById(tenantUserId)).thenReturn(true);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollService.initEnroll(tenantUserId, deviceId, fingerIndex))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispositivo no encontrado");
    }

    @Test
    void initEnroll_whenDeviceInactive_shouldThrow() {
        var inactiveDevice = Device.restore(deviceId, "DEV-001", "Device 1", null, null, null, null, false, LocalDateTime.now());
        when(tenantUserPort.existsById(tenantUserId)).thenReturn(true);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(inactiveDevice));

        assertThatThrownBy(() -> enrollService.initEnroll(tenantUserId, deviceId, fingerIndex))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dispositivo no está activo");
    }

    @Test
    void initEnroll_whenActiveSessionExists_shouldThrow() {
        var device = createActiveDevice();
        when(tenantUserPort.existsById(tenantUserId)).thenReturn(true);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(enrollSessionStore.existsActiveSession(tenantUserId, deviceId)).thenReturn(true);

        assertThatThrownBy(() -> enrollService.initEnroll(tenantUserId, deviceId, fingerIndex))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una sesión");
    }

    @Test
    void initEnroll_whenValid_shouldCreateSessionAndReturnResponse() {
        var device = createActiveDevice();
        when(tenantUserPort.existsById(tenantUserId)).thenReturn(true);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(enrollSessionStore.existsActiveSession(tenantUserId, deviceId)).thenReturn(false);

        var response = enrollService.initEnroll(tenantUserId, deviceId, fingerIndex);

        assertThat(response).isNotNull();
        assertThat(response.tenantUserId()).isEqualTo(tenantUserId);
        assertThat(response.deviceId()).isEqualTo(deviceId);
        assertThat(response.fingerIndex()).isEqualTo(fingerIndex);
        assertThat(response.deviceCode()).isEqualTo("DEV-001");
        assertThat(response.enrollToken()).isNotBlank();
        assertThat(response.expiresAt()).isNotNull();
        verify(enrollSessionStore).createSession(any(EnrollSession.class));
    }

    @Test
    void completeEnroll_whenTokenInvalid_shouldThrow() {
        when(enrollSessionStore.findAndConsume("invalid-token")).thenReturn(null);

        assertThatThrownBy(() -> enrollService.completeEnroll("invalid-token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token de enrolamiento inválido");
    }

    @Test
    void completeEnroll_whenFingerIndexMismatch_shouldThrow() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);

        assertThatThrownBy(() -> enrollService.completeEnroll("token", "DEV-001", 2, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("índice de huella no coincide");
    }

    @Test
    void completeEnroll_whenDeviceCodeMismatch_shouldThrow() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);

        assertThatThrownBy(() -> enrollService.completeEnroll("token", "WRONG-DEVICE", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("código de dispositivo no coincide");
    }

    @Test
    void completeEnroll_whenValidAndExistingBiometria_shouldDeactivateOldAndCreateNew() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        var existingBiometria = mock(UserBiometria.class);
        var savedBiometria = UserBiometria.restore(1L, tenantUserId, deviceId, "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now(), true, LocalDateTime.now(), LocalDateTime.now());

        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);
        when(userBiometriaRepository.findActiveByTenantUserIdAndFingerIndex(tenantUserId, 1)).thenReturn(List.of(existingBiometria));
        transportOpensAndSeals();
        when(userBiometriaRepository.save(any())).thenReturn(savedBiometria);

        var result = enrollService.completeEnroll("token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.tenantUserId()).isEqualTo(tenantUserId);
        verify(existingBiometria).deactivate();
        verify(userBiometriaRepository, times(2)).save(any());
    }

    @Test
    void completeEnroll_storesTheStoredTemplateInsteadOfTheTransportPayload() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);
        when(userBiometriaRepository.findActiveByTenantUserIdAndFingerIndex(tenantUserId, 1)).thenReturn(List.of());
        transportOpensAndSeals();
        when(userBiometriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        enrollService.completeEnroll("token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now());

        ArgumentCaptor<UserBiometria> saved = ArgumentCaptor.forClass(UserBiometria.class);
        verify(userBiometriaRepository).save(saved.capture());
        verify(biometricMatching).requireValidTemplate(PLAIN_TEMPLATE);
        assertThat(saved.getValue().getEncryptedTemplateBase64()).isEqualTo("c2VsbGFkbw==");
        assertThat(saved.getValue().getIvBase64()).isEqualTo("aXY=");
        assertThat(saved.getValue().getTagBase64()).isEqualTo("dGFn");
        assertThat(saved.getValue().getKeyRef()).isEqualTo("storage-v1");
        assertThat(saved.getValue().getEncryptedAesKeyBase64()).isNull();
    }

    // Reactivar desde PATCH /biometria una huella vieja deja dos filas activas del mismo dedo.
    @Test
    void completeEnroll_deactivatesEveryActiveRowOfThatFinger() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        var first = mock(UserBiometria.class);
        var second = mock(UserBiometria.class);
        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);
        when(userBiometriaRepository.findActiveByTenantUserIdAndFingerIndex(tenantUserId, 1)).thenReturn(List.of(first, second));
        transportOpensAndSeals();
        when(userBiometriaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        enrollService.completeEnroll("token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now());

        verify(first).deactivate();
        verify(second).deactivate();
        verify(userBiometriaRepository, times(3)).save(any());
    }

    @Test
    void completeEnroll_rejectsAPayloadThatCannotBeDecryptedWithoutTouchingTheExistingFinger() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);
        when(biometricCrypto.openTransport(any())).thenThrow(new BiometricDecryptionException("Error descifrado biométrico", null));

        assertThatThrownBy(() -> enrollService.completeEnroll("token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userBiometriaRepository, never()).save(any());
        verify(userBiometriaRepository, never()).findActiveByTenantUserIdAndFingerIndex(any(), any());
    }

    @Test
    void completeEnroll_rejectsATemplateThatIsNotSourceAfis() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);
        when(biometricCrypto.openTransport(any())).thenReturn(PLAIN_TEMPLATE);
        doThrow(new BiometricTemplateFormatException("no es SourceAFIS")).when(biometricMatching).requireValidTemplate(PLAIN_TEMPLATE);

        assertThatThrownBy(() -> enrollService.completeEnroll("token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userBiometriaRepository, never()).save(any());
    }

    @Test
    void findPendingSession_shouldReturnSession() {
        var deviceCode = "DEV-001";
        var session = new EnrollSession("token", tenantUserId, deviceId, fingerIndex, deviceCode, LocalDateTime.now().plusMinutes(1));
        when(enrollSessionStore.findPendingByDeviceCode(deviceCode)).thenReturn(Optional.of(session));

        var result = enrollService.findPendingSession(deviceCode);

        assertThat(result).isPresent();
        assertThat(result.get().enrollToken()).isEqualTo("token");
    }

    @Test
    void findPendingSession_shouldReturnEmptyWhenNotFound() {
        var deviceCode = "UNKNOWN";
        when(enrollSessionStore.findPendingByDeviceCode(deviceCode)).thenReturn(Optional.empty());

        var result = enrollService.findPendingSession(deviceCode);

        assertThat(result).isEmpty();
    }

    @Test
    void completeEnroll_whenValidAndNoExistingBiometria_shouldCreateNew() {
        var session = new EnrollSession("token", tenantUserId, deviceId, 1, "DEV-001", LocalDateTime.now().plusMinutes(1));
        var savedBiometria = UserBiometria.restore(1L, tenantUserId, deviceId, "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now(), true, LocalDateTime.now(), LocalDateTime.now());

        when(enrollSessionStore.findAndConsume("token")).thenReturn(session);
        when(userBiometriaRepository.findActiveByTenantUserIdAndFingerIndex(tenantUserId, 1)).thenReturn(List.of());
        transportOpensAndSeals();
        when(userBiometriaRepository.save(any())).thenReturn(savedBiometria);

        var result = enrollService.completeEnroll("token", "DEV-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(userBiometriaRepository, times(1)).save(any());
    }
}
