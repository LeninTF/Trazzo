package trazzo.back.corehr.domain.model.employee;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.InvalidTenantUserException;
import trazzo.back.corehr.domain.model.TenantUserState;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantUser {

    private Long id;
    private UUID masterUserId;
    private TenantUserState state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private TenantUser(
            Long id,
            UUID masterUserId,
            TenantUserState state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.masterUserId = requireMasterUserId(masterUserId);
        this.state = requireState(state);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static TenantUser create(UUID masterUserId) {
        LocalDateTime now = LocalDateTime.now();
        return new TenantUser(null, masterUserId, TenantUserState.ACTIVO, now, now, null);
    }

    public static TenantUser restore(
            Long id,
            UUID masterUserId,
            TenantUserState state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new TenantUser(id, masterUserId, state, createdAt, updatedAt, deletedAt);
    }

    public void activate() {
        if (state == TenantUserState.ACTIVO) {
            return;
        }
        this.state = TenantUserState.ACTIVO;
        touch();
    }

    public void setLicencia() {
        this.state = TenantUserState.LICENCIA;
        touch();
    }

    public void deactivate() {
        this.state = TenantUserState.INACTIVO;
        this.deletedAt = LocalDateTime.now(clock);
        touch();
    }

    public boolean isActive() {
        return state == TenantUserState.ACTIVO;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static UUID requireMasterUserId(UUID masterUserId) {
        if (masterUserId == null) {
            throw new InvalidTenantUserException("masterUserId is required");
        }
        return masterUserId;
    }

    private static TenantUserState requireState(TenantUserState state) {
        if (state == null) {
            throw new InvalidTenantUserException("state is required");
        }
        return state;
    }
}
