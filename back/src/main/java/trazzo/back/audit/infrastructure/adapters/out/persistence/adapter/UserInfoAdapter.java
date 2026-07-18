package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.UserInfoPort;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserInfoAdapter implements UserInfoPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<UserInfo> findByUserId(String userId) {
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
            UserInfo info = jdbcTemplate.queryForObject("""
                    SELECT u.id AS user_id,
                           p.name || ' ' || p.father_surname || ' ' || p.mother_surname AS user_name,
                           u.email AS user_email
                    FROM users u
                    JOIN persons p ON p.id = u.person_id
                    WHERE u.id = ? AND u.deleted_at IS NULL
                    """,
                    (rs, rowNum) -> new UserInfo(
                            rs.getString("user_id"),
                            rs.getString("user_name"),
                            rs.getString("user_email")
                    ),
                    new SqlParameterValue(Types.OTHER, uuid)
            );
            return Optional.ofNullable(info);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, UserInfo> findByUserIds(List<String> userIds) {
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
        String sql = "SELECT u.id AS user_id, " +
                "p.name || ' ' || p.father_surname || ' ' || p.mother_surname AS user_name, " +
                "u.email AS user_email " +
                "FROM users u " +
                "JOIN persons p ON p.id = u.person_id " +
                "WHERE u.id IN (" + placeholders + ") AND u.deleted_at IS NULL";
        SqlParameterValue[] params = uuids.stream()
                .map(u -> new SqlParameterValue(Types.OTHER, u))
                .toArray(SqlParameterValue[]::new);
        var result = new HashMap<String, UserInfo>();
        try {
            jdbcTemplate.query(sql, rs -> {
                String userId = rs.getString("user_id");
                result.put(userId, new UserInfo(
                        userId,
                        rs.getString("user_name"),
                        rs.getString("user_email")
                ));
            }, (Object[]) params);
        } catch (DataAccessException e) {
            return Map.of();
        }
        return result;
    }
}
