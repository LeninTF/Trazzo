package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.PlanFeatureRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.PlanFeature;

@Repository
@RequiredArgsConstructor
public class PlanFeatureJdbcRepositoryAdapter implements PlanFeatureRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public PlanFeature save(PlanFeature planFeature) {
        Integer id = jdbc.queryForObject(
                """
                INSERT INTO plan_features
                    (plan_id, feature_id, tipo_dato, value, date_start, is_active, created_at, updated_at)
                VALUES (?, ?, ?::tipo_dato_enum, ?::jsonb, ?, ?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                planFeature.getPlanId(),
                planFeature.getFeatureId(),
                planFeature.getDataType(),
                planFeature.getValue(),
                planFeature.getDateStart(),
                planFeature.isActive(),
                planFeature.getCreatedAt(),
                planFeature.getUpdatedAt());
        return PlanFeature.restore(id, planFeature.getPlanId(), planFeature.getFeatureId(),
                planFeature.getDataType(), planFeature.getValue(), planFeature.getDateStart(),
                planFeature.getDateEnd(), planFeature.isActive(),
                planFeature.getCreatedAt(), planFeature.getUpdatedAt());
    }

    @Override
    public List<PlanFeature> findByPlanId(Integer planId) {
        return jdbc.query(
                "SELECT * FROM plan_features WHERE plan_id = ? AND is_active = TRUE ORDER BY id",
                this::mapRow, planId);
    }

    @Override
    public void deleteByPlanId(Integer planId) {
        jdbc.update("DELETE FROM plan_features WHERE plan_id = ?", planId);
    }

    private PlanFeature mapRow(ResultSet rs, int rowNum) throws SQLException {
        return PlanFeature.restore(
                rs.getInt("id"),
                rs.getInt("plan_id"),
                rs.getInt("feature_id"),
                rs.getString("tipo_dato"),
                rs.getString("value"),
                rs.getObject("date_start", LocalDate.class),
                rs.getObject("date_end", LocalDate.class),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
