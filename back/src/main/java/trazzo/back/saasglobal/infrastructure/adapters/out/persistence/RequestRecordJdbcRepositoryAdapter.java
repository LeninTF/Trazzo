package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.RequestRecordRepositoryPort;
import trazzo.back.saasglobal.domain.model.request.RequestRecord;

@Repository
@RequiredArgsConstructor
public class RequestRecordJdbcRepositoryAdapter implements RequestRecordRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public RequestRecord save(RequestRecord record) {
        Integer id = jdbc.queryForObject(
                """
                INSERT INTO requests_record (request_id, status, user_id, change_reason, created_at)
                VALUES (?, ?::status_enum, ?::uuid, ?, ?)
                RETURNING id
                """,
                Integer.class,
                record.getRequestId(),
                record.getStatus(),
                record.getUserId(),
                record.getChangeReason(),
                record.getCreatedAt());
        return RequestRecord.restore(id, record.getRequestId(), record.getStatus(),
                record.getUserId(), record.getChangeReason(), record.getCreatedAt());
    }

    @Override
    public List<RequestRecord> findByRequestId(Integer requestId) {
        return jdbc.query(
                "SELECT * FROM requests_record WHERE request_id = ? ORDER BY created_at",
                this::mapRow, requestId);
    }

    private RequestRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return RequestRecord.restore(
                rs.getInt("id"),
                rs.getInt("request_id"),
                rs.getString("status"),
                rs.getString("user_id"),
                rs.getString("change_reason"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
