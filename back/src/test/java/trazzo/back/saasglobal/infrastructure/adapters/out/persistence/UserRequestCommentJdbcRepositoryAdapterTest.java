package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.request.UserRequestComment;

@ExtendWith(MockitoExtension.class)
class UserRequestCommentJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks UserRequestCommentJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsAndReturnsWithId() {
        UserRequestComment link = UserRequestComment.create("admin-1", 10);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(3);

        UserRequestComment saved = adapter.save(link);

        assertEquals(3, saved.getId());
        assertEquals("admin-1", saved.getUserId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRequestCommentId_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<UserRequestComment> result = adapter.findByRequestCommentId(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRequestCommentId_returnsMappedResult() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(3);
        when(rs.getString("user_id")).thenReturn("admin-1");
        when(rs.getInt("request_comment_id")).thenReturn(10);
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<UserRequestComment> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<UserRequestComment> result = adapter.findByRequestCommentId(10);

        assertTrue(result.isPresent());
        assertEquals("admin-1", result.get().getUserId());
    }
}
