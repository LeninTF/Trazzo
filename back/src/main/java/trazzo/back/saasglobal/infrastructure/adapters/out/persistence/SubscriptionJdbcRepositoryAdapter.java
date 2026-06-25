package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.SubscriptionRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;

@Repository
@RequiredArgsConstructor
public class SubscriptionJdbcRepositoryAdapter implements SubscriptionRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Subscription save(Subscription subscription) {
        jdbc.update("""
                INSERT INTO "Subscriptions"
                    ("id", "planId", "tenantId", "dateStart", "dateEnd", "status", "purchasePrice", "createdAt")
                VALUES (?, ?, ?::uuid, ?, ?, CAST(? AS subscription_status_enum), ?, ?)
                ON CONFLICT ("id") DO UPDATE SET
                    "planId"        = EXCLUDED."planId",
                    "tenantId"      = EXCLUDED."tenantId",
                    "dateStart"     = EXCLUDED."dateStart",
                    "dateEnd"       = EXCLUDED."dateEnd",
                    "status"        = EXCLUDED."status",
                    "purchasePrice" = EXCLUDED."purchasePrice"
                """,
                subscription.getId(),
                subscription.getPlanId(),
                subscription.getTenantId(),
                subscription.getDateStart(),
                subscription.getDateEnd(),
                subscription.getStatus().name(),
                subscription.getPurchasePrice(),
                subscription.getCreatedAt());
        return subscription;
    }

    @Override
    public Optional<Subscription> findById(String id) {
        List<Subscription> rows = jdbc.query(
                "SELECT * FROM \"Subscriptions\" WHERE \"id\" = ?::uuid",
                this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Subscription> findActiveByTenantId(String tenantId) {
        List<Subscription> rows = jdbc.query(
                "SELECT * FROM \"Subscriptions\" WHERE \"tenantId\" = ?::uuid"
                + " AND \"status\" IN ('TRIAL','ACTIVE') ORDER BY \"createdAt\" DESC LIMIT 1",
                this::mapRow, tenantId);
        return rows.stream().findFirst();
    }

    private Subscription mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Subscription.restore(
                rs.getString("id"),
                rs.getObject("planId", Integer.class),
                rs.getString("tenantId"),
                rs.getObject("dateStart", LocalDate.class),
                rs.getObject("dateEnd", LocalDate.class),
                SubscriptionStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("purchasePrice"),
                rs.getObject("createdAt", LocalDateTime.class));
    }
}
