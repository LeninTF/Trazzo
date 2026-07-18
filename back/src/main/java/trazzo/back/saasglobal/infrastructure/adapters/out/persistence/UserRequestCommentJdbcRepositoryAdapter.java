package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.UserRequestCommentRepositoryPort;
import trazzo.back.saasglobal.domain.model.request.UserRequestComment;

@Repository
@RequiredArgsConstructor
public class UserRequestCommentJdbcRepositoryAdapter implements UserRequestCommentRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public UserRequestComment save(UserRequestComment userRequestComment) {
        Integer id = jdbc.queryForObject(
                """
                INSERT INTO user_request_comment (user_id, request_comment_id, created_at)
                VALUES (?::uuid, ?, ?)
                RETURNING id
                """,
                Integer.class,
                userRequestComment.getUserId(),
                userRequestComment.getRequestCommentId(),
                userRequestComment.getCreatedAt());
        return UserRequestComment.restore(id, userRequestComment.getUserId(),
                userRequestComment.getRequestCommentId(), userRequestComment.getCreatedAt());
    }

    @Override
    public Optional<UserRequestComment> findByRequestCommentId(Integer requestCommentId) {
        List<UserRequestComment> rows = jdbc.query(
                "SELECT * FROM user_request_comment WHERE request_comment_id = ?",
                this::mapRow, requestCommentId);
        return rows.stream().findFirst();
    }

    private UserRequestComment mapRow(ResultSet rs, int rowNum) throws SQLException {
        return UserRequestComment.restore(
                rs.getInt("id"),
                rs.getString("user_id"),
                rs.getInt("request_comment_id"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
