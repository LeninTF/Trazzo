package trazzo.back.incidents.infrastructure.adapters.out.persistence;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.incidents.application.port.out.TenantUserPort;
//TODO: cambiar a el modulo de corehr
@Repository
@RequiredArgsConstructor
public class TenantUserJdbcAdapter implements TenantUserPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<TenantUserBasicInfo> findBasicInfoById(String tenantUserId) {
        String sql = """
                SELECT u.id, p.name, p.father_surname, p.mother_surname, u.email
                FROM users u
                JOIN persons p ON p.id = u.person_id
                WHERE u.id = ?::uuid AND u.deleted_at IS NULL
                """;
        return jdbc.query(sql, (rs, rowNum) ->
                new TenantUserBasicInfo(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("father_surname"),
                        rs.getString("mother_surname"),
                        rs.getString("email")
                ), tenantUserId).stream().findFirst();
    }
}
