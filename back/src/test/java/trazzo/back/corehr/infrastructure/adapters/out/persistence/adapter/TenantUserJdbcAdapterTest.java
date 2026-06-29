package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.corehr.application.port.out.TenantUserPort;

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
        var user = new TenantUserPort.TenantUserBasicInfo(1L, "Juan", "Perez", "Lopez", "juan@mail.com", "999888777");
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(user));

        var result = adapter.findBasicInfoById(1L);

        assertTrue(result.isPresent());
        assertEquals("Juan", result.get().nombre());
        assertEquals("999888777", result.get().phone());
    }

    @Test
    void findBasicInfoByIdReturnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of());

        var result = adapter.findBasicInfoById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByIdReturnsTrueWhenUserFound() {
        var user = new TenantUserPort.TenantUserBasicInfo(1L, "Juan", "Perez", "Lopez", "juan@mail.com", "999888777");
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(user));

        assertTrue(adapter.existsById(1L));
    }

    @Test
    void existsByIdReturnsFalseWhenUserNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of());

        assertFalse(adapter.existsById(99L));
    }

    @Test
    void findStateByIdReturnsState() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(trazzo.back.corehr.domain.model.TenantUserState.ACTIVO));

        var state = adapter.findStateById(1L);

        assertTrue(state.isPresent());
        assertEquals(trazzo.back.corehr.domain.model.TenantUserState.ACTIVO, state.get());
    }
}
