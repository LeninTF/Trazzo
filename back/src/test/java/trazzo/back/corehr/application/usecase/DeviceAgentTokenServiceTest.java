package trazzo.back.corehr.application.usecase;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.shared.security.AgentDeviceTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceAgentTokenServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 23, 9, 0);

    private final DeviceRepositoryPort devices = mock(DeviceRepositoryPort.class);
    private final AgentDeviceTokenRepository tokens = mock(AgentDeviceTokenRepository.class);
    private final DeviceAgentTokenService service = new DeviceAgentTokenService(devices, tokens);

    private void deviceSeven() {
        when(devices.findById(7L)).thenReturn(Optional.of(
                Device.restore(7L, "DVC-007", "Lector puerta", "192.168.1.7", 4370, "Ingreso", 1L, true, CREATED)));
        when(tokens.replace(eq("tenant_demo"), eq(7L), eq("DVC-007"), anyString()))
                .thenReturn(new AgentDeviceTokenRepository.Issued(TENANT, CREATED));
    }

    @Test
    void issueReturnsARandomDeviceTokenOnceAndStoresTheSameValue() {
        deviceSeven();

        var issued = service.issue("tenant_demo", 7L);

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(tokens).replace(eq("tenant_demo"), eq(7L), eq("DVC-007"), stored.capture());
        assertThat(issued.token()).matches("trzd_[A-Za-z0-9_-]{43}").isEqualTo(stored.getValue());
        assertThat(issued.tenantId()).isEqualTo(TENANT);
        assertThat(issued.deviceCode()).isEqualTo("DVC-007");
        assertThat(issued.createdAt()).isEqualTo(CREATED);
    }

    @Test
    void everyIssueIsADifferentToken() {
        deviceSeven();

        assertThat(service.issue("tenant_demo", 7L).token()).isNotEqualTo(service.issue("tenant_demo", 7L).token());
    }

    @Test
    void anUnknownDeviceGetsNoToken() {
        when(devices.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issue("tenant_demo", 9L)).isInstanceOf(IllegalArgumentException.class);
        verify(tokens, never()).replace(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void revokeRevokesEveryTokenOfTheDevice() {
        deviceSeven();

        service.revoke("tenant_demo", 7L);

        verify(tokens).revokeAll("tenant_demo", 7L);
    }

    @Test
    void revokingAnUnknownDeviceFails() {
        when(devices.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke("tenant_demo", 9L)).isInstanceOf(IllegalArgumentException.class);
        verify(tokens, never()).revokeAll(any(), anyLong());
    }
}
