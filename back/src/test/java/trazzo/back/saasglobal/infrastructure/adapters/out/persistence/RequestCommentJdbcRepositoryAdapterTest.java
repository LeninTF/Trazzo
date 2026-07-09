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
import trazzo.back.saasglobal.domain.model.request.RequestComments;

@ExtendWith(MockitoExtension.class)
class RequestCommentJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks RequestCommentJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsCommentAndReturnsWithId() {
        RequestComments comment = RequestComments.create(1, null, "hola");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any())).thenReturn(10);

        RequestComments saved = adapter.save(comment);

        assertEquals(10, saved.getId());
        assertEquals("hola", saved.getComment());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRequestId_returnsMappedComments() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(10);
        when(rs.getInt("request_id")).thenReturn(1);
        when(rs.getObject("request_contact_id")).thenReturn(null);
        when(rs.getString("comment")).thenReturn("hola");
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<RequestComments> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<RequestComments> result = adapter.findByRequestId(1);

        assertEquals(1, result.size());
        assertEquals("hola", result.get(0).getComment());
        assertNull(result.get(0).getRequestContactId());
    }
}
