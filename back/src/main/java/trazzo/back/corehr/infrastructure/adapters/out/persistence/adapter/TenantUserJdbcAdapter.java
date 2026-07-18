package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final String ID_BY_MASTER_USER_SQL = """
            SELECT tu.id FROM tenant_user tu
            WHERE tu.master_user_id = ? AND tu.deleted_at IS NULL
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
    public Optional<Long> findIdByMasterUserId(UUID masterUserId) {
        return jdbc.queryForList(ID_BY_MASTER_USER_SQL, Long.class, masterUserId)
                .stream().findFirst();
    }
}
