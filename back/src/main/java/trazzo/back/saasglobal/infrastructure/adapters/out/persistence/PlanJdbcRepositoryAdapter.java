package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.PlanRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

@Repository
@RequiredArgsConstructor
public class PlanJdbcRepositoryAdapter implements PlanRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Plan save(Plan plan) {
        if (plan.getId() == null) {
            Integer id = jdbc.queryForObject(
                    """
                    INSERT INTO plans (name, price, currency, billing_period, is_active, created_at, updated_at)
                    VALUES (?, ?, ?::currency_enum, ?, ?, ?, ?)
                    RETURNING id
                    """,
                    Integer.class,
                    plan.getName(),
                    plan.getPrice(),
                    plan.getCurrency(),
                    plan.getBillingPeriod(),
                    plan.isActive(),
                    plan.getCreatedAt(),
                    plan.getUpdatedAt());
            return Plan.restore(id, plan.getName(), plan.getPrice(), plan.getCurrency(),
                    plan.getBillingPeriod(), plan.isActive(),
                    plan.getCreatedAt(), plan.getUpdatedAt(), plan.getDeletedAt());
        }
        jdbc.update("""
                UPDATE plans
                SET name           = ?,
                    price          = ?,
                    currency       = ?::currency_enum,
                    billing_period = ?,
                    is_active      = ?,
                    updated_at     = ?,
                    deleted_at     = ?
                WHERE id = ?
                """,
                plan.getName(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingPeriod(),
                plan.isActive(),
                plan.getUpdatedAt(),
                plan.getDeletedAt(),
                plan.getId());
        return plan;
    }

    @Override
    public Optional<Plan> findById(Integer id) {
        List<Plan> rows = jdbc.query(
                "SELECT * FROM plans WHERE id = ?", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public List<Plan> findAll() {
        return jdbc.query("SELECT * FROM plans WHERE deleted_at IS NULL ORDER BY id", this::mapRow);
    }

    @Override
    public List<Plan> findAllActive() {
        return jdbc.query(
                "SELECT * FROM plans WHERE is_active = TRUE AND deleted_at IS NULL ORDER BY id", this::mapRow);
    }

    private Plan mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Plan.restore(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getBigDecimal("price"),
                rs.getString("currency"),
                rs.getString("billing_period"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class),
                rs.getObject("deleted_at", LocalDateTime.class));
    }
}
