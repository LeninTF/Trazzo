package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.UserInfoPort;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserInfoAdapter implements UserInfoPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<UserInfo> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            UserInfo info = jdbcTemplate.queryForObject("""
                    SELECT u.id AS user_id,
                           p.name || ' ' || p.father_surname || ' ' || p.mother_surname AS user_name,
                           u.email AS user_email
                    FROM users u
                    JOIN persons p ON p.id = u.person_id
                    WHERE u.id = ?::uuid AND u.deleted_at IS NULL
                    """,
                    (rs, rowNum) -> new UserInfo(
                            rs.getString("user_id"),
                            rs.getString("user_name"),
                            rs.getString("user_email")
                    ),
                    userId
            );
            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
