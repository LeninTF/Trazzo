package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.TenantUserState;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

class TenantUserJdbcAdapterTest {

    private JdbcTemplate jdbc;
    private TenantUserJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        adapter = new TenantUserJdbcAdapter(jdbc);
    }

    @Test
    void findBasicInfoByIdReturnsUser() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("name")).thenReturn("Juan");
        when(rs.getString("father_surname")).thenReturn("Perez");
        when(rs.getString("mother_surname")).thenReturn("Lopez");
        when(rs.getString("email")).thenReturn("juan@mail.com");
        when(rs.getString("phone")).thenReturn("999888777");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RowMapper<TenantUserPort.TenantUserBasicInfo>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), anyLong())).thenAnswer(invocation -> {
            RowMapper<TenantUserPort.TenantUserBasicInfo> mapper = captor.getValue();
            return List.of(mapper.mapRow(rs, 0));
        });

        var result = adapter.findBasicInfoById(1L);

        assertTrue(result.isPresent());
        assertEquals("Juan", result.get().nombre());
        assertEquals("Perez", result.get().apellidoPaterno());
        assertEquals("Lopez", result.get().apellidoMaterno());
        assertEquals("juan@mail.com", result.get().email());
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
    void findStateByIdReturnsState() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("state")).thenReturn("ACTIVO");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<RowMapper<TenantUserState>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(anyString(), captor.capture(), anyLong())).thenAnswer(invocation -> {
            RowMapper<TenantUserState> mapper = captor.getValue();
            return List.of(mapper.mapRow(rs, 0));
        });

        var state = adapter.findStateById(1L);

        assertTrue(state.isPresent());
        assertEquals(TenantUserState.ACTIVO, state.get());
    }

    @Test
    void findIdByMasterUserIdReturnsId() {
        UUID masterUserId = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(masterUserId))).thenReturn(List.of(42L));

        var result = adapter.findIdByMasterUserId(masterUserId);

        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    void findIdByMasterUserIdReturnsEmptyWhenNotFound() {
        UUID masterUserId = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(masterUserId))).thenReturn(List.of());

        var result = adapter.findIdByMasterUserId(masterUserId);

        assertTrue(result.isEmpty());
    }
}
