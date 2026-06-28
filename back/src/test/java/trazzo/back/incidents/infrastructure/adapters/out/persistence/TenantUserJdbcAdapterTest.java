package trazzo.back.incidents.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.incidents.application.port.out.TenantUserPort;

import java.util.List;

class TenantUserJdbcAdapterTest {

    private JdbcTemplate jdbc;
    private TenantUserJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        adapter = new TenantUserJdbcAdapter(jdbc);
    }

    @Test
    void findBasicInfoByIdReturnsUser() {
        var user = new TenantUserPort.TenantUserBasicInfo("u-1", "Juan", "Perez", "Lopez", "juan@mail.com");
        when(jdbc.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of(user));

        var result = adapter.findBasicInfoById("u-1");

        assertTrue(result.isPresent());
        assertEquals("Juan", result.get().nombre());
    }

    @Test
    void findBasicInfoByIdReturnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of());

        var result = adapter.findBasicInfoById("not-found");

        assertTrue(result.isEmpty());
    }
}
