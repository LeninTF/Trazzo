package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.HoldingRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;

@Repository
@RequiredArgsConstructor
public class HoldingJdbcRepositoryAdapter implements HoldingRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Holding save(Holding holding) {
        if (holding.getId() == null) {
            Integer id = jdbc.queryForObject(
                    """
                    INSERT INTO holding (tax_id, reason_social, state, type, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """,
                    Integer.class,
                    holding.getTaxId(),
                    holding.getLegalName(),
                    holding.isActive(),
                    toSqlType(holding.getType()),
                    holding.getCreatedAt(),
                    holding.getUpdatedAt());
            return Holding.restore(id, holding.getTaxId(), holding.getLegalName(),
                    holding.getType(), holding.isActive(),
                    holding.getCreatedAt(), holding.getUpdatedAt(), holding.getDeletedAt());
        }
        jdbc.update("""
                UPDATE holding
                SET tax_id        = ?,
                    reason_social = ?,
                    state         = ?,
                    type          = ?,
                    updated_at    = ?,
                    deleted_at    = ?
                WHERE id = ?
                """,
                holding.getTaxId(),
                holding.getLegalName(),
                holding.isActive(),
                toSqlType(holding.getType()),
                holding.getUpdatedAt(),
                holding.getDeletedAt(),
                holding.getId());
        return holding;
    }

    @Override
    public Optional<Holding> findById(Integer id) {
        List<Holding> rows = jdbc.query(
                "SELECT * FROM holding WHERE id = ?", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Holding> findByTaxId(String taxId) {
        List<Holding> rows = jdbc.query(
                "SELECT * FROM holding WHERE tax_id = ?", this::mapRow, taxId);
        return rows.stream().findFirst();
    }

    @Override
    public List<Holding> findAll() {
        return jdbc.query("SELECT * FROM holding WHERE deleted_at IS NULL ORDER BY id", this::mapRow);
    }

    @Override
    public boolean existsByTaxId(String taxId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM holding WHERE tax_id = ?", Integer.class, taxId);
        return count != null && count > 0;
    }

    private Holding mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Holding.restore(
                rs.getInt("id"),
                rs.getString("tax_id"),
                rs.getString("reason_social"),
                fromSqlType(rs.getString("type")),
                rs.getBoolean("state"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class),
                rs.getObject("deleted_at", LocalDateTime.class));
    }

    private static String toSqlType(HoldingType type) {
        return type == HoldingType.PUBLIC ? "PUBLICO" : "PRIVADO";
    }

    private static HoldingType fromSqlType(String type) {
        return "PUBLICO".equals(type) ? HoldingType.PUBLIC : HoldingType.PRIVATE;
    }
}
