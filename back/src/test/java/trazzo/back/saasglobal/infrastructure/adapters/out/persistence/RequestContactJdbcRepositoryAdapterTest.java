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
import trazzo.back.saasglobal.domain.model.request.RequestContact;

@ExtendWith(MockitoExtension.class)
class RequestContactJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks RequestContactJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsContactAndReturnsSameInstance() {
        RequestContact contact = RequestContact.create(1, "Ana", "Perez", "ana@example.com",
                "999999999", "20123456789", "Acme SAC");

        RequestContact saved = adapter.save(contact);

        assertSame(contact, saved);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRequestId_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<RequestContact> result = adapter.findByRequestId(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByRequestId_returnsMappedContact() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("request_id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Ana");
        when(rs.getString("last_name")).thenReturn("Perez");
        when(rs.getString("email")).thenReturn("ana@example.com");
        when(rs.getString("phone_number")).thenReturn("999999999");
        when(rs.getString("tax_id")).thenReturn("20123456789");
        when(rs.getString("company_name")).thenReturn("Acme SAC");
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);
        when(rs.getObject("updated_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<RequestContact> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<RequestContact> result = adapter.findByRequestId(1);

        assertTrue(result.isPresent());
        assertEquals("Ana", result.get().getName());
        assertEquals("20123456789", result.get().getTaxId());
    }

    @Test
    void countByTaxId_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("20123456789"))).thenReturn(2L);

        assertEquals(2L, adapter.countByTaxId("20123456789"));
    }

    @Test
    void existsRecentByTaxId_returnsTrueWhenCountPositive() {
        var since = LocalDateTime.now().minusMinutes(15);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("20123456789"), eq(since))).thenReturn(1);

        assertTrue(adapter.existsRecentByTaxId("20123456789", since));
    }

    @Test
    void existsRecentByTaxId_returnsFalseWhenCountZero() {
        var since = LocalDateTime.now().minusMinutes(15);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("20123456789"), eq(since))).thenReturn(0);

        assertFalse(adapter.existsRecentByTaxId("20123456789", since));
    }

    @Test
    void countByTaxId_returnsZeroWhenQueryReturnsNull() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("20123456789"))).thenReturn(null);

        assertEquals(0L, adapter.countByTaxId("20123456789"));
    }

    @Test
    void existsRecentByTaxId_returnsFalseWhenQueryReturnsNull() {
        var since = LocalDateTime.now().minusMinutes(15);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("20123456789"), eq(since))).thenReturn(null);

        assertFalse(adapter.existsRecentByTaxId("20123456789", since));
    }
}
