package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepositoryAdapter implements UserRepositoryPort {

    private static final String BASE_SELECT = """
            SELECT u.id, u.person_id, u.tenant_id, u.email, u.phone, u.password,
                   u.must_change_password, u.created_at, u.updated_at, u.deleted_at,
                   COALESCE(array_agg(DISTINCT r.name) FILTER (WHERE r.name IS NOT NULL), '{}') AS roles,
                   COALESCE(array_agg(DISTINCT p.code) FILTER (WHERE p.code IS NOT NULL), '{}') AS permissions
            FROM users u
            LEFT JOIN user_roles_master ur ON ur.user_id = u.id
            LEFT JOIN roles_master r ON r.id = ur.roles_master_id
            LEFT JOIN role_permissions_master rpm ON rpm.role_id = r.id
            LEFT JOIN permissions_master p ON p.id = rpm.permission_id
            """;

    private static final String GROUP_BY = """
            GROUP BY u.id, u.person_id, u.tenant_id, u.email, u.phone, u.password,
                     u.must_change_password, u.created_at, u.updated_at, u.deleted_at
            """;

    private static final String FILTER_WHERE = """
            WHERE u.tenant_id IS NULL AND u.deleted_at IS NULL
              AND (:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%')))
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            User user = jdbc.queryForObject(
                    BASE_SELECT + "WHERE u.email = ? AND u.deleted_at IS NULL\n" + GROUP_BY,
                    this::mapRow, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        List<User> rows = jdbc.query(
                BASE_SELECT + "WHERE u.id = ?::uuid\n" + GROUP_BY, this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public List<User> findAll(String search, int page, int size) {
        MapSqlParameterSource params = filterParams(search)
                .addValue("limit", size)
                .addValue("offset", Math.max(page, 0) * size);
        return namedJdbc.query(
                BASE_SELECT + FILTER_WHERE + GROUP_BY + " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset",
                params, this::mapRow);
    }

    @Override
    public long countAll(String search) {
        Long count = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM users u " + FILTER_WHERE, filterParams(search), Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public User save(User user) {
        jdbc.update("""
                INSERT INTO users (id, person_id, tenant_id, email, phone,
                                   password, must_change_password, created_at, updated_at)
                VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                user.getId(), user.getPersonId(), user.getTenantId(), user.getEmail(),
                user.getPhone(), user.getPassword(), user.isMustChangePassword(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
        return user;
    }

    @Override
    public User update(User user) {
        jdbc.update("""
                UPDATE users
                SET email = ?, phone = ?, must_change_password = ?, updated_at = ?, deleted_at = ?
                WHERE id = ?::uuid
                """,
                user.getEmail(), user.getPhone(), user.isMustChangePassword(),
                user.getUpdatedAt(), user.getDeletedAt(), user.getId());
        return user;
    }

    private static MapSqlParameterSource filterParams(String search) {
        return new MapSqlParameterSource()
                .addValue("search", search, Types.VARCHAR);
    }

    private User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return User.restore(
                rs.getString("id"),
                rs.getInt("person_id"),
                rs.getString("tenant_id"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("password"),
                toList(rs.getArray("roles")),
                toList(rs.getArray("permissions")),
                rs.getBoolean("must_change_password"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getTimestamp("deleted_at") != null
                        ? rs.getTimestamp("deleted_at").toLocalDateTime() : null
        );
    }

    private static List<String> toList(Array sqlArray) throws SQLException {
        String[] arr = sqlArray != null ? (String[]) sqlArray.getArray() : new String[0];
        return List.of(arr);
    }
}
