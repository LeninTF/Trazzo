package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.FeatureRepositoryPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;

@Repository
@RequiredArgsConstructor
public class FeatureJdbcRepositoryAdapter implements FeatureRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public Feature save(Feature feature) {
        if (feature.getId() == null) {
            Integer id = jdbc.queryForObject(
                    """
                    INSERT INTO features (name, description, created_at, updated_at)
                    VALUES (?, ?, ?, ?)
                    RETURNING id
                    """,
                    Integer.class,
                    feature.getName(),
                    feature.getDescription(),
                    feature.getCreatedAt(),
                    feature.getUpdatedAt());
            return Feature.restore(id, feature.getName(), feature.getDescription(),
                    feature.getCreatedAt(), feature.getUpdatedAt());
        }
        jdbc.update("UPDATE features SET name = ?, description = ?, updated_at = ? WHERE id = ?",
                feature.getName(),
                feature.getDescription(),
                feature.getUpdatedAt(),
                feature.getId());
        return feature;
    }

    @Override
    public Optional<Feature> findById(Integer id) {
        List<Feature> rows = jdbc.query(
                "SELECT * FROM features WHERE id = ?", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public List<Feature> findAll() {
        return jdbc.query("SELECT * FROM features ORDER BY id", this::mapRow);
    }

    @Override
    public void deleteById(Integer id) {
        jdbc.update("DELETE FROM features WHERE id = ?", id);
    }

    private Feature mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Feature.restore(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
