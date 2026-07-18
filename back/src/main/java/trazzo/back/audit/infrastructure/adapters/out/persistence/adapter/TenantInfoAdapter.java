package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.TenantInfoPort;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TenantInfoAdapter implements TenantInfoPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<TenantInfo> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        try {
            TenantInfo info = jdbcTemplate.queryForObject("""
                    SELECT t.id AS tenant_id, t.sub_domain AS tenant_name
                    FROM users u
                    JOIN tenants t ON t.id = u.tenant_id
                    WHERE u.id = ? AND u.deleted_at IS NULL AND t.deleted_at IS NULL
                    """,
                    (rs, rowNum) -> new TenantInfo(
                            rs.getString("tenant_id"),
                            rs.getString("tenant_name")
                    ),
                    new SqlParameterValue(Types.OTHER, uuid)
            );
            return Optional.ofNullable(info);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, TenantInfo> findByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> uuids = userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(id -> {
                    try {
                        return UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        if (uuids.isEmpty()) {
            return Map.of();
        }
        String placeholders = uuids.stream().map(u -> "?").collect(Collectors.joining(","));
        String sql = "SELECT u.id AS user_id, t.id AS tenant_id, t.sub_domain AS tenant_name " +
                "FROM users u " +
                "JOIN tenants t ON t.id = u.tenant_id " +
                "WHERE u.id IN (" + placeholders + ") AND u.deleted_at IS NULL AND t.deleted_at IS NULL";
        SqlParameterValue[] params = uuids.stream()
                .map(u -> new SqlParameterValue(Types.OTHER, u))
                .toArray(SqlParameterValue[]::new);
        var result = new HashMap<String, TenantInfo>();
        try {
            jdbcTemplate.query(sql, rs -> {
                String userId = rs.getString("user_id");
                result.put(userId, new TenantInfo(
                        rs.getString("tenant_id"),
                        rs.getString("tenant_name")
                ));
            }, (Object[]) params);
        } catch (DataAccessException e) {
            return Map.of();
        }
        return result;
    }
}
