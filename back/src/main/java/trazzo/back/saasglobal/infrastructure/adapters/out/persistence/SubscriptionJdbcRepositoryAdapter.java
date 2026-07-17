package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;

@Repository
@RequiredArgsConstructor
public class SubscriptionJdbcRepositoryAdapter implements SubscriptionRepositoryPort {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public Subscription save(Subscription subscription) {
        jdbc.update("""
                INSERT INTO subscriptions
                    (id, plan_id, tenant_id, date_start, date_end, status, purchase_price, mp_preapproval_id, created_at)
                VALUES (?::uuid, ?, ?::uuid, ?, ?, CAST(? AS subscription_status_enum), ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    plan_id            = EXCLUDED.plan_id,
                    tenant_id          = EXCLUDED.tenant_id,
                    date_start         = EXCLUDED.date_start,
                    date_end           = EXCLUDED.date_end,
                    status             = EXCLUDED.status,
                    purchase_price     = EXCLUDED.purchase_price,
                    mp_preapproval_id  = EXCLUDED.mp_preapproval_id
                """,
                subscription.getId(),
                subscription.getPlanId(),
                subscription.getTenantId(),
                subscription.getDateStart(),
                subscription.getDateEnd(),
                subscription.getStatus().name(),
                subscription.getPurchasePrice(),
                subscription.getMpPreapprovalId(),
                subscription.getCreatedAt());
        return subscription;
    }

    @Override
    public Optional<Subscription> findById(String id) {
        List<Subscription> rows = jdbc.query(
                "SELECT * FROM subscriptions WHERE id = ?::uuid",
                this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Subscription> findActiveByTenantId(String tenantId) {
        List<Subscription> rows = jdbc.query(
                "SELECT * FROM subscriptions WHERE tenant_id = ?::uuid"
                + " AND status IN ('TRIAL','ACTIVE') ORDER BY created_at DESC LIMIT 1",
                this::mapRow, tenantId);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Subscription> findByMpPreapprovalId(String mpPreapprovalId) {
        List<Subscription> rows = jdbc.query(
                "SELECT * FROM subscriptions WHERE mp_preapproval_id = ?",
                this::mapRow, mpPreapprovalId);
        return rows.stream().findFirst();
    }

    @Override
    public List<Subscription> findAll(int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", Math.max(page, 0) * size);
        return namedJdbc.query(
                "SELECT * FROM subscriptions ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                params, this::mapRow);
    }

    @Override
    public long countAll() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM subscriptions", Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public void deleteByTenantId(String tenantId) {
        jdbc.update("DELETE FROM subscriptions WHERE tenant_id = ?::uuid", tenantId);
    }

    private Subscription mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Subscription.restore(
                rs.getString("id"),
                rs.getObject("plan_id", Integer.class),
                rs.getString("tenant_id"),
                rs.getObject("date_start", LocalDate.class),
                rs.getObject("date_end", LocalDate.class),
                SubscriptionStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("purchase_price"),
                rs.getString("mp_preapproval_id"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
