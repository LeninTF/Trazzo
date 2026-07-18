package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

@ExtendWith(MockitoExtension.class)
class RoleMasterJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks RoleMasterJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsNewRoleAndReturnsWithId() {
        RoleMaster role = RoleMaster.create("soporte", "Soporte", "desc");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any())).thenReturn(7);

        RoleMaster saved = adapter.save(role);

        assertEquals(7, saved.getId());
        assertEquals("soporte", saved.getName());
    }

    @Test
    void save_updatesExistingRole() {
        RoleMaster role = RoleMaster.restore(2, "soporte", "Soporte", "desc", List.of());

        RoleMaster saved = adapter.save(role);

        assertSame(role, saved);
        verify(jdbc).update(anyString(), eq("soporte"), eq("Soporte"), eq("desc"), eq(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<RoleMaster> result = adapter.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_mapsPermissionCodes() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        Array permsArray = mock(Array.class);
        when(permsArray.getArray()).thenReturn(new String[]{"gestion-tenants.crear"});
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("soporte");
        when(rs.getString("display_name")).thenReturn("Soporte");
        when(rs.getString("description")).thenReturn("desc");
        when(rs.getArray("permission_codes")).thenReturn(permsArray);

        when(jdbc.query(anyString(), any(RowMapper.class)))
                .thenAnswer(inv -> {
                    RowMapper<RoleMaster> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<RoleMaster> result = adapter.findAll();

        assertEquals(1, result.size());
        assertEquals(List.of("gestion-tenants.crear"), result.get(0).getPermissionCodes());
    }

    @Test
    void replacePermissions_deletesThenInsertsEach() {
        adapter.replacePermissions(2, List.of("gestion-tenants.crear", "monitoreo-sistema.dashboard-global"));

        verify(jdbc).update(contains("DELETE FROM role_permissions_master"), eq(2));
        verify(jdbc).update(contains("INSERT INTO role_permissions_master"), eq(2), eq("gestion-tenants.crear"));
        verify(jdbc).update(contains("INSERT INTO role_permissions_master"), eq(2), eq("monitoreo-sistema.dashboard-global"));
    }

    @Test
    void isAssignedToAnyUser_returnsTrueWhenExists() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(2))).thenReturn(true);

        assertTrue(adapter.isAssignedToAnyUser(2));
    }

    @Test
    void deleteById_executesDelete() {
        adapter.deleteById(2);

        verify(jdbc).update(anyString(), eq(2));
    }
}
