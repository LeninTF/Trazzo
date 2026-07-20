package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.UserInfoPort;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
                           COALESCE(p.name, '') || ' ' || COALESCE(p.father_surname, '') || ' ' || COALESCE(p.mother_surname, '') AS user_name,
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
                .filter(Objects::nonNull)
                .toList();
        if (uuids.isEmpty()) {
            return Map.of();
        }
        String sql = "SELECT u.id AS user_id, " +
                "COALESCE(p.name, '') || ' ' || COALESCE(p.father_surname, '') || ' ' || COALESCE(p.mother_surname, '') AS user_name, " +
                "u.email AS user_email " +
                "FROM users u " +
                "JOIN persons p ON p.id = u.person_id " +
                "WHERE u.id IN (:ids) AND u.deleted_at IS NULL";
        var namedParams = new NamedParameterJdbcTemplate(jdbcTemplate);
        var paramSource = new MapSqlParameterSource("ids", uuids);
        var result = new HashMap<String, UserInfo>();
        try {
            namedParams.query(sql, paramSource, rs -> {
                String uid = rs.getString("user_id");
                result.put(uid, new UserInfo(
                        uid,
                        rs.getString("user_name"),
                        rs.getString("user_email")
                ));
            });
        } catch (DataAccessException e) {
            return Map.of();
        }
        return result;
    }
}
