package trazzo.back.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.shared.tenancy.TenantContext;

import java.util.Optional;
import java.util.UUID;

/** El tenant_user vigente de un usuario maestro en el schema del tenant de la petición. */
@Repository
@RequiredArgsConstructor
public class TenantUserIdResolver {

    private final JdbcTemplate jdbc;

    public Optional<Long> resolve(UUID masterUserId) {
        if (masterUserId == null || TenantContext.DEFAULT_SCHEMA.equals(TenantContext.get())) {
            return Optional.empty();
        }
        return jdbc.query("""
                SELECT id FROM tenant_user WHERE master_user_id = ?::uuid AND deleted_at IS NULL
                """, (rs, rowNum) -> rs.getLong("id"), masterUserId.toString()).stream().findFirst();
    }
}
