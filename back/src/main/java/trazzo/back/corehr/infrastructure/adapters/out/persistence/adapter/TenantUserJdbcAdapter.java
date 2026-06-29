package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.TenantUserState;

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

    @Override
    public Optional<TenantUserState> findStateById(String tenantUserId) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(String tenantUserId) {
        return findBasicInfoById(tenantUserId).isPresent();
    }
}
