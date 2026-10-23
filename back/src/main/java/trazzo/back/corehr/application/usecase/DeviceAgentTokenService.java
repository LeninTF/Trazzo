package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.result.IssuedAgentToken;
import trazzo.back.corehr.application.port.out.DeviceRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.Device;
import trazzo.back.shared.security.AgentDeviceTokenRepository;
import trazzo.back.shared.security.DeviceTokenAuthFilter;

import java.security.SecureRandom;
import java.util.Base64;

/** Credencial de máquina del agente de cada lector: revocable y ligada al dispositivo y al tenant. */
@RequiredArgsConstructor
public class DeviceAgentTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeviceRepositoryPort deviceRepository;
    private final AgentDeviceTokenRepository tokenRepository;

    /** Emite un token nuevo (trzd_ + 32 bytes aleatorios en Base64url) y revoca el anterior del dispositivo. */
    public IssuedAgentToken issue(String tenantSchema, Long deviceId) {
        Device device = requireDevice(deviceId);
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String token = DeviceTokenAuthFilter.TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        var issued = tokenRepository.replace(tenantSchema, device.getId(), device.getCode(), token);
        return new IssuedAgentToken(token, issued.tenantId(), device.getCode(), issued.createdAt());
    }

    public void revoke(String tenantSchema, Long deviceId) {
        tokenRepository.revokeAll(tenantSchema, requireDevice(deviceId).getId());
    }

    private Device requireDevice(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo no encontrado: " + deviceId));
    }
}
