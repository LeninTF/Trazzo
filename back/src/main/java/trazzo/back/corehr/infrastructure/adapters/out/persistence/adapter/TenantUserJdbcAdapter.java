package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.TenantUserState;

@Repository
@RequiredArgsConstructor
public class TenantUserJdbcAdapter implements TenantUserPort {

    private final JdbcTemplate jdbc;

    private static final String BASIC_INFO_SQL = """
            SELECT tu.id, p.name, p.father_surname, p.mother_surname, u.email, u.phone
            FROM tenant_user tu
            JOIN users u ON u.id = tu.master_user_id
            JOIN persons p ON p.id = u.person_id
            WHERE tu.id = ? AND tu.deleted_at IS NULL AND u.deleted_at IS NULL
            """;

    private static final String STATE_SQL = """
            SELECT tu.state
            FROM tenant_user tu
            WHERE tu.id = ? AND tu.deleted_at IS NULL
            """;

    private static final String PROFILE_SELECT = """
            SELECT tu.id, u.email, u.phone, tu.state, u.must_change_password,
                   tu.created_at, tu.updated_at,
                   p.id AS person_id, p.document_type, p.document_value,
                   p.name, p.father_surname, p.mother_surname,
                   r.id AS role_id, r.name AS role_name
            FROM tenant_user tu
            JOIN users u ON u.id = tu.master_user_id
            JOIN persons p ON p.id = u.person_id
            LEFT JOIN tenant_user_role tur ON tur.tenant_user_id = tu.id
            LEFT JOIN role r ON r.id = tur.role_id
            """;

    private static final String COUNT_SQL = """
            SELECT COUNT(*) FROM tenant_user tu
            JOIN users u ON u.id = tu.master_user_id
            JOIN persons p ON p.id = u.person_id
            WHERE tu.deleted_at IS NULL
            """;

    @Override
    public Optional<TenantUserBasicInfo> findBasicInfoById(Long tenantUserId) {
        return jdbc.query(BASIC_INFO_SQL, (rs, rowNum) ->
                new TenantUserBasicInfo(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("father_surname"),
                        rs.getString("mother_surname"),
                        rs.getString("email"),
                        rs.getString("phone")
                ), tenantUserId).stream().findFirst();
    }

    @Override
    public Optional<TenantUserState> findStateById(Long tenantUserId) {
        return jdbc.query(STATE_SQL,
                (rs, rowNum) -> TenantUserState.valueOf(rs.getString("state")),
                tenantUserId).stream().findFirst();
    }

    @Override
    public boolean existsById(Long tenantUserId) {
        return findBasicInfoById(tenantUserId).isPresent();
    }

    @Override
    public List<TenantUserProfileProjection> findAllProfiles(String search, String status, int page, int size, String sort) {
        var sql = new StringBuilder(PROFILE_SELECT);
        var params = new ArrayList<>();
        var conditions = new ArrayList<String>();
        conditions.add("tu.deleted_at IS NULL");

        if (search != null && !search.isBlank()) {
            conditions.add("(LOWER(p.name) LIKE ? OR LOWER(p.father_surname) LIKE ? OR LOWER(u.email) LIKE ?)");
            var pattern = "%" + search.toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.isBlank()) {
            conditions.add("tu.state = ?");
            params.add(status);
        }

        sql.append(" WHERE ");
        sql.append(String.join(" AND ", conditions));

        if (sort != null && !sort.isBlank()) {
            sql.append(" ORDER BY ").append(mapSort(sort));
        } else {
            sql.append(" ORDER BY tu.created_at DESC");
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add((long) page * size);

        return jdbc.query(sql.toString(), new TenantUserProfileRowMapper(), params.toArray());
    }

    @Override
    public long countAllProfiles(String search, String status) {
        var sql = new StringBuilder(COUNT_SQL);
        var params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(p.name) LIKE ? OR LOWER(p.father_surname) LIKE ? OR LOWER(u.email) LIKE ?)");
            var pattern = "%" + search.toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND tu.state = ?");
            params.add(status);
        }

        var result = jdbc.queryForObject(sql.toString(), Long.class, params.toArray());
        return result != null ? result : 0L;
    }

    @Override
    public Optional<TenantUserProfileProjection> findProfileById(Long id) {
        var sql = PROFILE_SELECT + " WHERE tu.id = ? AND tu.deleted_at IS NULL";
        return jdbc.query(sql, new TenantUserProfileRowMapper(), id).stream().findFirst();
    }

    @Override
    public Long saveTenantUser(UUID masterUserId) {
        jdbc.update("""
                INSERT INTO tenant_user (master_user_id, state, created_at, updated_at)
                VALUES (?::uuid, 'ACTIVO', NOW(), NOW())
                """, masterUserId.toString());
        var id = jdbc.queryForObject("SELECT currval('tenant_user_id_seq')", Long.class);
        return id != null ? id : 0L;
    }

    @Override
    public void updateState(Long id, TenantUserState state) {
        jdbc.update("""
                UPDATE tenant_user SET state = ?, updated_at = NOW() WHERE id = ?
                """, state.name(), id);
    }

    @Override
    public void softDelete(Long id) {
        jdbc.update("""
                UPDATE tenant_user SET state = 'INACTIVO', deleted_at = NOW(), updated_at = NOW() WHERE id = ?
                """, id);
    }

    @Override
    public void hardDelete(Long id) {
        jdbc.update("""
                DELETE FROM tenant_user WHERE id = ?
                """, id);
    }

    @Override
    public void assignRole(Long tenantUserId, String roleId) {
        removeRole(tenantUserId);
        jdbc.update("""
                INSERT INTO tenant_user_role (tenant_user_id, role_id, created_at)
                VALUES (?, ?::uuid, NOW())
                """, tenantUserId, roleId);
    }

    @Override
    public void removeRole(Long tenantUserId) {
        jdbc.update("""
                DELETE FROM tenant_user_role WHERE tenant_user_id = ?
                """, tenantUserId);
    }

    @Override
    public Optional<String> findRoleIdByTenantUserId(Long tenantUserId) {
        var results = jdbc.query("""
                SELECT role_id FROM tenant_user_role WHERE tenant_user_id = ?
                """, (rs, rowNum) -> rs.getString("role_id"), tenantUserId);
        return results.stream().findFirst();
    }

    @Override
    public Integer savePerson(String documentType, String documentValue, String name, String fatherSurname, String motherSurname) {
        jdbc.update("""
                INSERT INTO persons (document_type, document_value, name, father_surname, mother_surname)
                VALUES (?, ?, ?, ?, ?)
                """, documentType, documentValue, name, fatherSurname, motherSurname);
        return jdbc.queryForObject("SELECT LASTVAL()", Integer.class);
    }

    @Override
    public void updatePerson(Integer personId, String name, String fatherSurname, String motherSurname) {
        jdbc.update("""
                UPDATE persons SET name = ?, father_surname = ?, mother_surname = ? WHERE id = ?
                """, name, fatherSurname, motherSurname, personId);
    }

    @Override
    public Optional<Integer> findPersonIdByDocument(String documentType, String documentValue) {
        var results = jdbc.query("""
                SELECT id FROM persons WHERE document_type = ? AND document_value = ?
                """, (rs, rowNum) -> rs.getInt("id"), documentType, documentValue);
        return results.stream().findFirst();
    }

    private String mapSort(String sort) {
        var parts = sort.split(",");
        var mapped = new ArrayList<String>();
        for (var part : parts) {
            var trimmed = part.trim();
            var asc = true;
            var field = trimmed;
            if (trimmed.startsWith("-")) {
                asc = false;
                field = trimmed.substring(1);
            } else if (trimmed.startsWith("+")) {
                field = trimmed.substring(1);
            }
            var col = switch (field) {
                case "name" -> "p.name";
                case "email" -> "u.email";
                case "createdAt", "created_at" -> "tu.created_at";
                case "updatedAt", "updated_at" -> "tu.updated_at";
                default -> "tu.created_at";
            };
            mapped.add(col + (asc ? " ASC" : " DESC"));
        }
        return String.join(", ", mapped);
    }

    private static class TenantUserProfileRowMapper implements RowMapper<TenantUserProfileProjection> {
        @Override
        public TenantUserProfileProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
            var createdAt = rs.getTimestamp("created_at");
            var updatedAt = rs.getTimestamp("updated_at");
            return new TenantUserProfileProjection(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("state"),
                    rs.getBoolean("must_change_password"),
                    createdAt != null ? createdAt.toLocalDateTime() : null,
                    updatedAt != null ? updatedAt.toLocalDateTime() : null,
                    rs.getInt("person_id"),
                    rs.getString("document_type"),
                    rs.getString("document_value"),
                    rs.getString("name"),
                    rs.getString("father_surname"),
                    rs.getString("mother_surname"),
                    rs.getString("role_id"),
                    rs.getString("role_name")
            );
        }
    }
}
