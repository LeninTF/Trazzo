package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.TenantInfoPort;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantInfoAdapter implements TenantInfoPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<TenantInfo> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            TenantInfo info = jdbcTemplate.queryForObject("""
                    SELECT t.id AS tenant_id, t.sub_domain AS tenant_name
                    FROM users u
                    JOIN tenants t ON t.id = u.tenant_id
                    WHERE u.id = ?::uuid AND u.deleted_at IS NULL AND t.deleted_at IS NULL
                    """,
                    (rs, rowNum) -> new TenantInfo(
                            rs.getString("tenant_id"),
                            rs.getString("tenant_name")
                    ),
                    userId
            );
            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
