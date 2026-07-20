package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.RequestRepositoryPort;
import trazzo.back.saasglobal.domain.model.request.Request;

@Repository
@RequiredArgsConstructor
public class RequestJdbcRepositoryAdapter implements RequestRepositoryPort {

    private static final String FILTER_WHERE = """
            WHERE (:status IS NULL OR status = CAST(:status AS status_enum))
              AND (:type IS NULL OR type = CAST(:type AS type_enum))
              AND (:search IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
                   OR LOWER(message) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%')))
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Override
    public Request save(Request request) {
        if (request.getId() == null) {
            Integer id = jdbc.queryForObject(
                    """
                    INSERT INTO requests (type, title, message, status, created_at, updated_at)
                    VALUES (?::type_enum, ?, ?, ?::status_enum, ?, ?)
                    RETURNING id
                    """,
                    Integer.class,
                    request.getType().name(),
                    request.getTitle(),
                    request.getMessage(),
                    request.getStatus().name(),
                    request.getCreatedAt(),
                    request.getUpdatedAt());
            return Request.restore(id, request.getType(), request.getTitle(), request.getMessage(),
                    request.getStatus(), request.getCreatedAt(), request.getUpdatedAt());
        }
        jdbc.update("""
                UPDATE requests
                SET status = ?::status_enum,
                    updated_at = ?
                WHERE id = ?
                """,
                request.getStatus().name(),
                request.getUpdatedAt(),
                request.getId());
        return request;
    }

    @Override
    public Optional<Request> findById(Integer id) {
        List<Request> rows = jdbc.query("SELECT * FROM requests WHERE id = ?", this::mapRow, id);
        return rows.stream().findFirst();
    }

    @Override
    public List<Request> findByFilters(Request.Status status, Request.Type type, String search, int page, int size) {
        MapSqlParameterSource params = filterParams(status, type, search)
                .addValue("limit", size)
                .addValue("offset", Math.max(page, 0) * size);
        return namedJdbc.query(
                "SELECT * FROM requests " + FILTER_WHERE + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset",
                params, this::mapRow);
    }

    @Override
    public long countByFilters(Request.Status status, Request.Type type, String search) {
        Long count = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM requests " + FILTER_WHERE,
                filterParams(status, type, search), Long.class);
        return count != null ? count : 0L;
    }

    // Explicit Types.VARCHAR is required even for null values: PostgreSQL cannot infer a bind
    // parameter's type from a bare "? IS NULL" check alone, and a genuinely-null Java value
    // gives MapSqlParameterSource no runtime type to infer either — without this hint the
    // driver fails with "could not determine data type of parameter".
    private static MapSqlParameterSource filterParams(Request.Status status, Request.Type type, String search) {
        return new MapSqlParameterSource()
                .addValue("status", status != null ? status.name() : null, Types.VARCHAR)
                .addValue("type", type != null ? type.name() : null, Types.VARCHAR)
                .addValue("search", search, Types.VARCHAR);
    }

    private Request mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Request.restore(
                rs.getInt("id"),
                Request.Type.valueOf(rs.getString("type")),
                rs.getString("title"),
                rs.getString("message"),
                Request.Status.valueOf(rs.getString("status")),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
