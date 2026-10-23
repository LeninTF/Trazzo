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
import java.util.stream.Stream;

/** Reglas de acceso de los endpoints de tenant, para {@code @PreAuthorize("@authz.…")}. */
@Component("authz")
@RequiredArgsConstructor
public class TenantAuthorization {

    public static final String TENANT_ADMIN = "administrador_tenant";

    // Grupos de lectura de la matriz permiso → endpoint; "modulo.*" es cualquier permiso del módulo.
    private static final String[] STAFF_READERS = {"gestion-trabajadores.*", "estructura-organizacional.asignar-personal",
            "control-asistencia.*", "gestion-horarios.asignacion-masiva", "reportes.*", "permisos-licencias.*",
            "incidencias.aprobar-rechazar"};
    private static final String[] STRUCTURE_READERS = Stream.concat(Arrays.stream(STAFF_READERS),
            Stream.of("estructura-organizacional.*", "gestion-horarios.*")).toArray(String[]::new);
    private static final String[] SCHEDULE_READERS = {"gestion-horarios.*", "horarios-docente.*", "control-asistencia.*",
            "reportes.*"};
    private static final String[] ALL_ATTENDANCE_READERS = {"control-asistencia.*", "reportes.*",
            "asistencia-docente.ver-tardanzas-faltas"};
    private static final String[] INCIDENT_APPROVERS = {"incidencias.aprobar-rechazar",
            "permisos-licencias.aprobar-incidencias", "permisos-licencias.rechazar-incidencias"};

    private final TenantUserIdResolver tenantUserIdResolver;

    /** El administrador del tenant pasa siempre; el resto necesita alguno de los permisos. */
    public boolean any(String... permissions) {
        Set<String> granted = grantedAuthorities();
        return granted.contains(TENANT_ADMIN)
                || Arrays.stream(permissions).anyMatch(permission -> holds(granted, permission));
    }

    /** VER_PERSONAL: ver el personal del tenant. */
    public boolean canViewStaff() {
        return any(STAFF_READERS);
    }

    /** VER_ESTRUCTURA: ver sedes, áreas y departamentos. */
    public boolean canViewStructure() {
        return any(STRUCTURE_READERS);
    }

    /** VER_HORARIOS: ver turnos, horarios, tolerancias y asignaciones. */
    public boolean canViewSchedules() {
        return any(SCHEDULE_READERS);
    }

    /** VER_ASISTENCIA_TODOS: ver la asistencia de todo el tenant, no solo la propia. */
    public boolean canViewAllAttendance() {
        return any(ALL_ATTENDANCE_READERS);
    }

    /** APROBADOR: ver y resolver las incidencias de todo el tenant. */
    public boolean approver() {
        return any(INCIDENT_APPROVERS);
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

    private static boolean holds(Set<String> granted, String permission) {
        if (permission.endsWith(".*")) {
            String module = permission.substring(0, permission.length() - 1);
            return granted.stream().anyMatch(authority -> authority.startsWith(module));
        }
        return granted.contains(permission);
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
