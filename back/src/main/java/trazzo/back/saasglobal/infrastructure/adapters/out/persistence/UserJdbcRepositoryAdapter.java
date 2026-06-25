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
                SELECT u."id", u."personId", u."tenantId", u."email", u."phone", u."password",
                       u."createdAt", u."updatedAt", u."deletedAt",
                       COALESCE(array_agg(r."name") FILTER (WHERE r."name" IS NOT NULL), '{}') AS roles
                FROM "Users" u
                LEFT JOIN "UserRolesMaster" ur ON ur."userId" = u."id"
                LEFT JOIN "RolesMaster" r ON r."id" = ur."rolesMasterId"
                WHERE u."email" = ? AND u."deletedAt" IS NULL
                GROUP BY u."id", u."personId", u."tenantId", u."email", u."phone", u."password",
                         u."createdAt", u."updatedAt", u."deletedAt"
                """;
        try {
            User user = jdbc.queryForObject(sql, (rs, rowNum) -> {
                Array rolesArray = rs.getArray("roles");
                String[] rolesArr = rolesArray != null ? (String[]) rolesArray.getArray() : new String[0];
                return User.restore(
                        rs.getString("id"),
                        rs.getInt("personId"),
                        rs.getString("tenantId"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("password"),
                        List.of(rolesArr),
                        rs.getTimestamp("createdAt").toLocalDateTime(),
                        rs.getTimestamp("updatedAt").toLocalDateTime(),
                        rs.getTimestamp("deletedAt") != null
                                ? rs.getTimestamp("deletedAt").toLocalDateTime() : null
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
                INSERT INTO "Users" ("id", "personId", "tenantId", "email", "phone",
                                     "password", "createdAt", "updatedAt")
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                user.getId(), user.getPersonId(), user.getTenantId(), user.getEmail(),
                user.getPhone(), user.getPassword(), user.getCreatedAt(), user.getUpdatedAt()
        );
        return user;
    }
}
