package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.request.RequestRecord;

@ExtendWith(MockitoExtension.class)
class RequestRecordJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks RequestRecordJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsRecordAndReturnsWithId() {
        RequestRecord record = RequestRecord.create(1, "APPROVED", "admin-1", "looks good");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any(), any())).thenReturn(7);

        RequestRecord saved = adapter.save(record);

        assertEquals(7, saved.getId());
        assertEquals("APPROVED", saved.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRequestId_returnsMappedRecords() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(7);
        when(rs.getInt("request_id")).thenReturn(1);
        when(rs.getString("status")).thenReturn("APPROVED");
        when(rs.getString("user_id")).thenReturn("admin-1");
        when(rs.getString("change_reason")).thenReturn("looks good");
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<RequestRecord> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<RequestRecord> result = adapter.findByRequestId(1);

        assertEquals(1, result.size());
        assertEquals("APPROVED", result.get(0).getStatus());
        assertEquals("admin-1", result.get(0).getUserId());
    }
}
