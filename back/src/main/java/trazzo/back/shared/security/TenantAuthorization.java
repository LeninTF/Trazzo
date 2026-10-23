package trazzo.back.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Reglas de acceso de los endpoints de tenant, para {@code @PreAuthorize("@authz.…")}. */
@Component("authz")
@RequiredArgsConstructor
public class TenantAuthorization {

    public static final String TENANT_ADMIN = "administrador_tenant";

    private final TenantUserIdResolver tenantUserIdResolver;

    /** El administrador del tenant pasa siempre; el resto necesita alguno de los permisos. */
    public boolean any(String... permissions) {
        Set<String> granted = grantedAuthorities();
        return granted.contains(TENANT_ADMIN) || Arrays.stream(permissions).anyMatch(granted::contains);
    }

    public boolean admin() {
        return grantedAuthorities().contains(TENANT_ADMIN);
    }

    public boolean agent() {
        return grantedAuthorities().contains(DeviceAgentPrincipal.AUTHORITY);
    }

    public Optional<Long> currentTenantUserId() {
        return authentication()
                .map(Authentication::getPrincipal)
                .filter(AuthenticatedUser.class::isInstance)
                .map(AuthenticatedUser.class::cast)
                .flatMap(user -> tenantUserIdResolver.resolve(user.id()));
    }

    public boolean isSelf(Long tenantUserId) {
        return tenantUserId != null && currentTenantUserId().filter(tenantUserId::equals).isPresent();
    }

    private static Optional<Authentication> authentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated);
    }

    private static Set<String> grantedAuthorities() {
        return authentication()
                .map(authentication -> authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }
}
