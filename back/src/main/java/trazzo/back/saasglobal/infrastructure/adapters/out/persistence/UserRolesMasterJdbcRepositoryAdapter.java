package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.UserRolesMasterRepositoryPort;

@Repository
@RequiredArgsConstructor
public class UserRolesMasterJdbcRepositoryAdapter implements UserRolesMasterRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public List<Integer> findRoleIdsForUser(String userId) {
        return jdbc.queryForList(
                "SELECT roles_master_id FROM user_roles_master WHERE user_id = ?::uuid",
                Integer.class, userId);
    }

    @Override
    public void replaceForUser(String userId, List<Integer> roleIds) {
        jdbc.update("DELETE FROM user_roles_master WHERE user_id = ?::uuid", userId);
        for (Integer roleId : roleIds) {
            jdbc.update(
                    "INSERT INTO user_roles_master (user_id, roles_master_id) VALUES (?::uuid, ?)",
                    userId, roleId);
        }
    }
}
