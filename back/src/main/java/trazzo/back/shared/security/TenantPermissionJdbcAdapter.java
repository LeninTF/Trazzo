package trazzo.back.shared.security;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.shared.tenancy.TenantContext;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TenantPermissionJdbcAdapter implements TenantPermissionPort {

    // Los nombres de permiso los escribe el administrador del tenant y se usan como autoridades: uno que
    // empiece por ROLE_ se leería como un rol de Spring (ROLE_SAAS_ADMIN abre /saas/** de toda la plataforma).
    private static final String SPRING_ROLE_PREFIX = "ROLE_";

    private final JdbcTemplate jdbc;

    @Override
    public List<String> findPermissionCodesByMasterUserId(UUID masterUserId) {
        String schema = TenantContext.get();
        if (schema == null || "public".equals(schema)) {
            return List.of();
        }
        List<String> codes = jdbc.queryForList(
                """
                SELECT p.name
                FROM tenant_user tu
                JOIN tenant_user_role tur ON tur.tenant_user_id = tu.id
                JOIN role r ON r.id = tur.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE tu.master_user_id = ?::uuid AND tu.deleted_at IS NULL
                """,
                String.class,
                masterUserId.toString()
        );
        List<String> permissions = codes.stream().filter(code -> !code.startsWith(SPRING_ROLE_PREFIX)).toList();
        if (permissions.size() < codes.size()) {
            log.warn("Se ignoran {} permisos del tenant {} con prefijo {}: no son autoridades",
                    codes.size() - permissions.size(), schema, SPRING_ROLE_PREFIX);
        }
        return permissions;
    }
}
