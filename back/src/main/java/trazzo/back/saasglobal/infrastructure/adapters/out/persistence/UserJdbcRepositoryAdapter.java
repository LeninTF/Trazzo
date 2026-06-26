package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.Array;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepositoryAdapter implements UserRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT u.id, u.person_id, u.tenant_id, u.email, u.phone, u.password,
                       u.created_at, u.updated_at, u.deleted_at,
                       COALESCE(array_agg(r.name) FILTER (WHERE r.name IS NOT NULL), '{}') AS roles
                FROM users u
                LEFT JOIN user_roles_master ur ON ur.user_id = u.id
                LEFT JOIN roles_master r ON r.id = ur.roles_master_id
                WHERE u.email = ? AND u.deleted_at IS NULL
                GROUP BY u.id, u.person_id, u.tenant_id, u.email, u.phone, u.password,
                         u.created_at, u.updated_at, u.deleted_at
                """;
        try {
            User user = jdbc.queryForObject(sql, (rs, rowNum) -> {
                Array rolesArray = rs.getArray("roles");
                String[] rolesArr = rolesArray != null ? (String[]) rolesArray.getArray() : new String[0];
                return User.restore(
                        rs.getString("id"),
                        rs.getInt("person_id"),
                        rs.getString("tenant_id"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("password"),
                        List.of(rolesArr),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        rs.getTimestamp("deleted_at") != null
                                ? rs.getTimestamp("deleted_at").toLocalDateTime() : null
                );
            }, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User save(User user) {
        jdbc.update("""
                INSERT INTO users (id, person_id, tenant_id, email, phone,
                                   password, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                user.getId(), user.getPersonId(), user.getTenantId(), user.getEmail(),
                user.getPhone(), user.getPassword(), user.getCreatedAt(), user.getUpdatedAt()
        );
        return user;
    }
}
