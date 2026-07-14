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
    public Optional<TenantInfo> findByTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        try {
            TenantInfo info = jdbcTemplate.queryForObject("""
                    SELECT id AS tenant_id, sub_domain AS tenant_name
                    FROM tenants
                    WHERE id = ?::uuid AND deleted_at IS NULL
                    """,
                    (rs, rowNum) -> new TenantInfo(
                            rs.getString("tenant_id"),
                            rs.getString("tenant_name")
                    ),
                    tenantId
            );
            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
