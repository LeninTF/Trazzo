package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.RoleMasterRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

@Repository
@RequiredArgsConstructor
public class RoleMasterJdbcRepositoryAdapter implements RoleMasterRepositoryPort {

    private static final String SELECT_WITH_PERMISSIONS = """
            SELECT r.id, r.name, r.display_name, r.description,
                   COALESCE(array_agg(p.code) FILTER (WHERE p.code IS NOT NULL), '{}') AS permission_codes
            FROM roles_master r
            LEFT JOIN role_permissions_master rpm ON rpm.role_id = r.id
            LEFT JOIN permissions_master p ON p.id = rpm.permission_id
            """;

    private final JdbcTemplate jdbc;

    @Override
    public RoleMaster save(RoleMaster role) {
        if (role.getId() == null) {
            Integer id = jdbc.queryForObject(
                    "INSERT INTO roles_master (name, display_name, description) VALUES (?, ?, ?) RETURNING id",
                    Integer.class, role.getName(), role.getDisplayName(), role.getDescription());
            return RoleMaster.restore(id, role.getName(), role.getDisplayName(), role.getDescription(),
                    role.getPermissionCodes());
        }
        jdbc.update("UPDATE roles_master SET name = ?, display_name = ?, description = ? WHERE id = ?",
                role.getName(), role.getDisplayName(), role.getDescription(), role.getId());
        return role;
    }

    @Override
    public Optional<RoleMaster> findById(Integer id) {
        List<RoleMaster> rows = jdbc.query(
                SELECT_WITH_PERMISSIONS + " WHERE r.id = ? GROUP BY r.id, r.name, r.display_name, r.description",
                this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<RoleMaster> findByName(String name) {
        List<RoleMaster> rows = jdbc.query(
                SELECT_WITH_PERMISSIONS + " WHERE r.name = ? GROUP BY r.id, r.name, r.display_name, r.description",
                this::mapRow, name);
        return rows.stream().findFirst();
    }

    @Override
    public List<RoleMaster> findAll() {
        return jdbc.query(
                SELECT_WITH_PERMISSIONS + " GROUP BY r.id, r.name, r.display_name, r.description ORDER BY r.id",
                this::mapRow);
    }

    @Override
    public void deleteById(Integer id) {
        jdbc.update("DELETE FROM roles_master WHERE id = ?", id);
    }

    @Override
    public void replacePermissions(Integer roleId, List<String> permissionCodes) {
        jdbc.update("DELETE FROM role_permissions_master WHERE role_id = ?", roleId);
        for (String code : permissionCodes) {
            jdbc.update("""
                    INSERT INTO role_permissions_master (role_id, permission_id)
                    SELECT ?, id FROM permissions_master WHERE code = ?
                    """, roleId, code);
        }
    }

    @Override
    public boolean isAssignedToAnyUser(Integer roleId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM user_roles_master WHERE roles_master_id = ?)",
                Boolean.class, roleId);
        return Boolean.TRUE.equals(exists);
    }

    private RoleMaster mapRow(ResultSet rs, int rowNum) throws SQLException {
        Array permsArray = rs.getArray("permission_codes");
        String[] perms = permsArray != null ? (String[]) permsArray.getArray() : new String[0];
        return RoleMaster.restore(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("display_name"),
                rs.getString("description"),
                List.of(perms));
    }
}
