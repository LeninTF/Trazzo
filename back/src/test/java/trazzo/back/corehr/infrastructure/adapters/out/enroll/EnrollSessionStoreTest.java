package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** SQL y parámetros; el comportamiento contra PostgreSQL lo cubre EnrollSessionStoreIntegrationTest. */
@ExtendWith(MockitoExtension.class)
class EnrollSessionStoreTest {

    @Mock
    private JdbcTemplate jdbc;

    private EnrollSessionStore store() {
        return new EnrollSessionStore(jdbc);
    }

    @SuppressWarnings("unchecked")
    private static RowMapper<EnrollSession> anyMapper() {
        return any(RowMapper.class);
    }

    // Un único DELETE … RETURNING: dos peticiones con el mismo token no pueden consumirlo ambas.
    @Test
    void findAndConsume_isASingleDeleteReturningStatement() {
        var session = new EnrollSession("t-1", 10L, 5L, 3, "DVC-001", LocalDateTime.now().plusMinutes(1));
        when(jdbc.query(argThat((String sql) -> sql.startsWith("DELETE FROM enroll_session WHERE enroll_token = ?")
                && sql.contains("RETURNING")), anyMapper(), eq("t-1"))).thenReturn(List.of(session));

        assertThat(store().findAndConsume("t-1")).isEqualTo(session);
        verify(jdbc).query(any(String.class), anyMapper(), eq("t-1"));
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void findAndConsume_aSessionThatHadExpiredIsDeletedButNotReturned() {
        var expired = new EnrollSession("t-2", 10L, 5L, 3, "DVC-001", LocalDateTime.now().minusSeconds(1));
        when(jdbc.query(any(String.class), anyMapper(), eq("t-2"))).thenReturn(List.of(expired));

        assertThat(store().findAndConsume("t-2")).isNull();
    }

    @Test
    void createSession_purgesExpiredSessionsAndInsertsEveryField() {
        var expires = LocalDateTime.now().plusMinutes(2);

        store().createSession(new EnrollSession("t-3", 10L, 5L, 3, "DVC-001", expires));

        var order = inOrder(jdbc);
        order.verify(jdbc).update(eq("DELETE FROM enroll_session WHERE expires_at <= ?"), any(LocalDateTime.class));
        order.verify(jdbc).update(argThat((String sql) -> sql.startsWith("INSERT INTO enroll_session")),
                eq("t-3"), eq(10L), eq(5L), eq(3), eq("DVC-001"), eq(expires));
    }
}
