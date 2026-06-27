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
    public Optional<Plan> findById(Integer id) {
        List<Plan> rows = jdbc.query(
                "SELECT * FROM plans WHERE id = ?", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public List<Plan> findAll() {
        return jdbc.query("SELECT * FROM plans ORDER BY id", this::mapRow);
    }

    @Override
    public List<Plan> findAllActive() {
        return jdbc.query("SELECT * FROM plans WHERE is_active = TRUE ORDER BY id", this::mapRow);
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
