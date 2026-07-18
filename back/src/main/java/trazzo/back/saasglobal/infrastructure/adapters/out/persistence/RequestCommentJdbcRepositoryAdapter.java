package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.RequestCommentRepositoryPort;
import trazzo.back.saasglobal.domain.model.request.RequestComments;

@Repository
@RequiredArgsConstructor
public class RequestCommentJdbcRepositoryAdapter implements RequestCommentRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public RequestComments save(RequestComments comment) {
        Integer id = jdbc.queryForObject(
                """
                INSERT INTO request_comments (request_id, request_contact_id, comment, created_at)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """,
                Integer.class,
                comment.getRequestId(),
                comment.getRequestContactId(),
                comment.getComment(),
                comment.getCreatedAt());
        return RequestComments.restore(id, comment.getRequestId(), comment.getRequestContactId(),
                comment.getComment(), comment.getCreatedAt());
    }

    @Override
    public List<RequestComments> findByRequestId(Integer requestId) {
        return jdbc.query(
                "SELECT * FROM request_comments WHERE request_id = ? ORDER BY created_at",
                this::mapRow, requestId);
    }

    private RequestComments mapRow(ResultSet rs, int rowNum) throws SQLException {
        return RequestComments.restore(
                rs.getInt("id"),
                rs.getInt("request_id"),
                (Integer) rs.getObject("request_contact_id"),
                rs.getString("comment"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
