package trazzo.back.shared.security;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.shared.tenancy.TenantContext;

@Repository
@RequiredArgsConstructor
public class TenantPermissionJdbcAdapter implements TenantPermissionPort {

    private final JdbcTemplate jdbc;

    @Override
    public List<String> findPermissionCodesByMasterUserId(UUID masterUserId) {
        String schema = TenantContext.get();
        if (schema == null || "public".equals(schema)) {
            return List.of();
        }
        return jdbc.queryForList(
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
    }
}
