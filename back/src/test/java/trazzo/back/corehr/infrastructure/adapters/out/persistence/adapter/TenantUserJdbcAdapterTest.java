package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.TenantUserPort.TenantUserProfileProjection;
import trazzo.back.corehr.domain.model.TenantUserState;

import java.time.LocalDateTime;
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
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(TenantUserState.ACTIVO));

        var state = adapter.findStateById(1L);

        assertTrue(state.isPresent());
        assertEquals(TenantUserState.ACTIVO, state.get());
    }

    @Test
    void findStateByIdReturnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of());

        var state = adapter.findStateById(99L);

        assertTrue(state.isEmpty());
    }

    @Test
    void findAllProfiles_withoutFilters_shouldReturnAll() {
        var projection = new TenantUserProfileProjection(
                1L, "email@mail.com", "999888777", "ACTIVO", false,
                LocalDateTime.now(), LocalDateTime.now(),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez",
                UUID.randomUUID().toString(), "administrador"
        );
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles(null, null, 0, 10, null);

        assertEquals(1, results.size());
        assertEquals("Juan", results.get(0).name());
    }

    @Test
    void findAllProfiles_withSearchAndStatus_shouldFilter() {
        var projection = new TenantUserProfileProjection(
                1L, "email@mail.com", "999888777", "ACTIVO", false,
                LocalDateTime.now(), LocalDateTime.now(),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez", null, null
        );
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles("juan", "ACTIVO", 0, 10, "name");

        assertEquals(1, results.size());
    }

    @Test
    void findAllProfiles_withSortDesc_shouldApplySort() {
        var projection = new TenantUserProfileProjection(
                1L, "email@mail.com", "999888777", "ACTIVO", false,
                LocalDateTime.now(), LocalDateTime.now(),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez", null, null
        );
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles(null, null, 0, 10, "-name");

        assertEquals(1, results.size());
    }

    @Test
    void countAllProfiles_withoutFilters_shouldReturnCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(5L);

        var count = adapter.countAllProfiles(null, null);

        assertEquals(5L, count);
    }

    @Test
    void countAllProfiles_withFilters_shouldReturnCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(3L);

        var count = adapter.countAllProfiles("juan", "ACTIVO");

        assertEquals(3L, count);
    }

    @Test
    void countAllProfiles_shouldReturnZeroWhenResultIsNull() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(null);

        var count = adapter.countAllProfiles(null, null);

        assertEquals(0L, count);
    }

    @Test
    void findProfileById_shouldReturnProjection() {
        var projection = new TenantUserProfileProjection(
                1L, "email@mail.com", "999888777", "ACTIVO", false,
                LocalDateTime.now(), LocalDateTime.now(),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez", null, null
        );
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(projection));

        var result = adapter.findProfileById(1L);

        assertTrue(result.isPresent());
        assertEquals("email@mail.com", result.get().email());
    }

    @Test
    void findProfileById_shouldReturnEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of());

        var result = adapter.findProfileById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void saveTenantUser_shouldInsertAndReturnId() {
        var uuid = UUID.randomUUID();
        when(jdbc.update(anyString(), anyString())).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(42L);

        var id = adapter.saveTenantUser(uuid);

        assertEquals(42L, id);
    }

    @Test
    void saveTenantUser_shouldReturnZeroWhenNoSeqValue() {
        var uuid = UUID.randomUUID();
        when(jdbc.update(anyString(), anyString())).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        var id = adapter.saveTenantUser(uuid);

        assertEquals(0L, id);
    }

    @Test
    void updateState_shouldExecuteUpdate() {
        adapter.updateState(1L, TenantUserState.INACTIVO);

        verify(jdbc).update(anyString(), eq("INACTIVO"), eq(1L));
    }

    @Test
    void softDelete_shouldExecuteUpdate() {
        adapter.softDelete(1L);

        verify(jdbc).update(anyString(), eq(1L));
    }

    @Test
    void hardDelete_shouldExecuteUpdate() {
        adapter.hardDelete(1L);

        verify(jdbc).update(anyString(), eq(1L));
    }

    @Test
    void assignRole_shouldRemoveThenInsert() {
        var roleId = UUID.randomUUID().toString();
        adapter.assignRole(1L, roleId);

        verify(jdbc).update(contains("DELETE"), eq(1L));
        verify(jdbc).update(contains("INSERT"), eq(1L), eq(roleId));
    }

    @Test
    void removeRole_shouldExecuteDelete() {
        adapter.removeRole(1L);

        verify(jdbc).update(anyString(), eq(1L));
    }

    @Test
    void findRoleIdByTenantUserId_shouldReturnRoleId() {
        var roleId = UUID.randomUUID().toString();
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(roleId));

        var result = adapter.findRoleIdByTenantUserId(1L);

        assertTrue(result.isPresent());
        assertEquals(roleId, result.get());
    }

    @Test
    void findRoleIdByTenantUserId_shouldReturnEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of());

        var result = adapter.findRoleIdByTenantUserId(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void savePerson_shouldInsertAndReturnId() {
        when(jdbc.update(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(100);

        var id = adapter.savePerson("DNI", "12345678", "Juan", "Perez", "Lopez");

        assertEquals(100, id);
    }

    @Test
    void updatePerson_shouldExecuteUpdate() {
        adapter.updatePerson(1, "Juan", "Perez", "Lopez");

        verify(jdbc).update(anyString(), eq("Juan"), eq("Perez"), eq("Lopez"), eq(1));
    }

    @Test
    void findPersonIdByDocument_shouldReturnId() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyString()))
                .thenReturn(List.of(100));

        var result = adapter.findPersonIdByDocument("DNI", "12345678");

        assertTrue(result.isPresent());
        assertEquals(100, result.get());
    }

    @Test
    void findPersonIdByDocument_shouldReturnEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyString()))
                .thenReturn(List.of());

        var result = adapter.findPersonIdByDocument("DNI", "UNKNOWN");

        assertTrue(result.isEmpty());
    }
}
