package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.TenantUserState;

class TenantUserJdbcAdapterTest {

    private JdbcTemplate jdbc;
    private TenantUserJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        adapter = new TenantUserJdbcAdapter(jdbc);
    }

    @Test
    void findBasicInfoById_returnsEmpty_whenNoRow() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(99L))).thenReturn(List.of());

        assertThat(adapter.findBasicInfoById(99L)).isEmpty();
    }

    @Test
    void findBasicInfoById_returnsInfo_whenRowFound() {
        var info = new TenantUserPort.TenantUserBasicInfo(1L, "Ana", "Perez", "Lopez",
                "ana@trazzo.pe", "999999999");
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of(info));

        var result = adapter.findBasicInfoById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().nombre()).isEqualTo("Ana");
        assertThat(result.get().email()).isEqualTo("ana@trazzo.pe");
    }

    @Test
    void findStateById_returnsEmpty_whenNoRow() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(5L))).thenReturn(List.of());

        assertThat(adapter.findStateById(5L)).isEmpty();
    }

    @Test
    void findStateById_returnsState_whenRowFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(TenantUserState.ACTIVO));

        assertThat(adapter.findStateById(1L)).contains(TenantUserState.ACTIVO);
    }

    @Test
    void existsById_returnsTrue_whenBasicInfoPresent() {
        var info = new TenantUserPort.TenantUserBasicInfo(1L, "Ana", "P", "L", "a@x.pe", "111");
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of(info));

        assertThat(adapter.existsById(1L)).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenBasicInfoMissing() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(2L))).thenReturn(List.of());

        assertThat(adapter.existsById(2L)).isFalse();
    }

    @Test
    void findIdByMasterUserId_returnsEmpty_whenNoRow() {
        var uuid = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(uuid.toString())))
                .thenReturn(List.of());

        assertThat(adapter.findIdByMasterUserId(uuid)).isEmpty();
    }

    @Test
    void findIdByMasterUserId_returnsId_whenRowFound() {
        var uuid = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), eq(uuid.toString())))
                .thenReturn(List.of(7L));

        assertThat(adapter.findIdByMasterUserId(uuid)).contains(7L);
    }

    @Test
    void findAllProfiles_noFilters_usesDefaultOrderAndPagination() {
        var projection = sampleProjection(1L, "user@x.pe", "ACTIVO");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles(null, null, 0, 10, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).email()).isEqualTo("user@x.pe");
    }

    @Test
    void findAllProfiles_withSearch_addsSearchCondition() {
        var projection = sampleProjection(1L, "ana@x.pe", "ACTIVO");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles("ana", null, 0, 10, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void findAllProfiles_withStatus_addsStatusCondition() {
        var projection = sampleProjection(1L, "ana@x.pe", "ACTIVO");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles(null, "ACTIVO", 0, 10, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void findAllProfiles_withBlankSearchAndStatus_ignoresBlankFilters() {
        var projection = sampleProjection(1L, "ana@x.pe", "ACTIVO");
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(projection));

        var results = adapter.findAllProfiles("   ", "   ", 0, 10, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void findAllProfiles_withSortName_usesMappedSortField() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "name");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void findAllProfiles_withSortNameDesc_usesDescOrder() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "-name");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void findAllProfiles_withSortEmailPlusPrefix_stripsPlus() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "+email");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void findAllProfiles_withSortCreatedAt_mapsToCreatedAtAlias() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "created_at,asc");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void findAllProfiles_withSortUpdatedAt_mapsToUpdatedAtAlias() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "updated_at");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void findAllProfiles_withUnknownSort_fallsBackToCreatedAt() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "unknownField");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void findAllProfiles_withMultipleSortFields_appendsAll() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAllProfiles(null, null, 0, 10, "name,email");

        verify(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void countAllProfiles_noFilters_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(42L);

        assertThat(adapter.countAllProfiles(null, null)).isEqualTo(42L);
    }

    @Test
    void countAllProfiles_withSearch_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(5L);

        assertThat(adapter.countAllProfiles("ana", null)).isEqualTo(5L);
    }

    @Test
    void countAllProfiles_withStatus_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(3L);

        assertThat(adapter.countAllProfiles(null, "ACTIVO")).isEqualTo(3L);
    }

    @Test
    void countAllProfiles_returnsZero_whenJdbcReturnsNull() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);

        assertThat(adapter.countAllProfiles(null, null)).isZero();
    }

    @Test
    void findProfileById_returnsEmpty_whenNoRow() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(99L))).thenReturn(List.of());

        assertThat(adapter.findProfileById(99L)).isEmpty();
    }

    @Test
    void findProfileById_returnsProfile_whenRowFound() {
        var projection = sampleProjection(1L, "ana@x.pe", "ACTIVO");
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of(projection));

        var result = adapter.findProfileById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("ana@x.pe");
    }

    @Test
    void findOrgAssignmentsByUserIds_returnsEmptyMap_whenInputEmpty() {
        var result = adapter.findOrgAssignmentsByUserIds(List.of());

        assertThat(result).isEmpty();
        verify(jdbc, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    void findOrgAssignmentsByUserIds_buildsBundlePerUser_evenWhenNoAssignments() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(inv -> List.of());

        var result = adapter.findOrgAssignmentsByUserIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L).sedes()).isEmpty();
        assertThat(result.get(1L).areas()).isEmpty();
        assertThat(result.get(1L).departamentos()).isEmpty();
    }

    @Test
    void findOrgAssignmentsByUserIds_aggregatesRows_byUserId() {
        Object[] row1 = {1L,
                new TenantUserPort.OrgAssignmentRow(10L, "Sede Norte"),
                new TenantUserPort.OrgAssignmentRow(100L, "Operaciones"),
                new TenantUserPort.OrgAssignmentRow(1000L, "Deps")};
        Object[] row2 = {1L,
                new TenantUserPort.OrgAssignmentRow(11L, "Sede Sur"),
                new TenantUserPort.OrgAssignmentRow(101L, "Logistica"),
                new TenantUserPort.OrgAssignmentRow(1001L, "Almacen")};
        Object[] row3 = {2L,
                new TenantUserPort.OrgAssignmentRow(12L, "Sede Este"),
                new TenantUserPort.OrgAssignmentRow(102L, "Ventas"),
                new TenantUserPort.OrgAssignmentRow(1002L, "Comercial")};

        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(inv -> List.of((Object) row1, (Object) row2, (Object) row3));

        var result = adapter.findOrgAssignmentsByUserIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L).sedes()).hasSize(2);
        assertThat(result.get(1L).areas()).hasSize(2);
        assertThat(result.get(1L).departamentos()).hasSize(2);
        assertThat(result.get(2L).sedes()).hasSize(1);
        assertThat(result.get(2L).sedes().get(0).nombre()).isEqualTo("Sede Este");
    }

    @Test
    void findOrgAssignmentsByUserIds_onlyReturnsDataForRequestedIds() {
        Object[] row = {99L,
                new TenantUserPort.OrgAssignmentRow(1L, "Sede"),
                new TenantUserPort.OrgAssignmentRow(2L, "Area"),
                new TenantUserPort.OrgAssignmentRow(3L, "Depto")};

        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(inv -> List.of((Object) row));

        var result = adapter.findOrgAssignmentsByUserIds(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(1L);
        assertThat(result).doesNotContainKey(99L);
        assertThat(result.get(1L).sedes()).isEmpty();
    }

    @Test
    void saveTenantUser_insertsAndReturnsSequenceId() {
        var uuid = UUID.randomUUID();
        when(jdbc.queryForObject(eq("SELECT currval('tenant_user_id_seq')"), eq(Long.class)))
                .thenReturn(42L);

        var id = adapter.saveTenantUser(uuid);

        assertThat(id).isEqualTo(42L);
        verify(jdbc).update(anyString(), eq(uuid.toString()));
    }

    @Test
    void saveTenantUser_returnsZero_whenSequenceIsNull() {
        var uuid = UUID.randomUUID();
        when(jdbc.queryForObject(eq("SELECT currval('tenant_user_id_seq')"), eq(Long.class)))
                .thenReturn(null);

        var id = adapter.saveTenantUser(uuid);

        assertThat(id).isZero();
    }

    @Test
    void updateState_executesUpdateWithStateName() {
        adapter.updateState(5L, TenantUserState.INACTIVO);

        verify(jdbc).update(anyString(), eq("INACTIVO"), eq(5L));
    }

    @Test
    void softDelete_executesUpdateSoftDeleting() {
        adapter.softDelete(7L);

        verify(jdbc).update(anyString(), eq(7L));
    }

    @Test
    void hardDelete_executesPhysicalDelete() {
        adapter.hardDelete(9L);

        verify(jdbc).update(anyString(), eq(9L));
    }

    @Test
    void assignRole_removesExistingThenInsertsNew() {
        adapter.assignRole(5L, "role-uuid-1");

        verify(jdbc).update(anyString(), eq(5L));
        verify(jdbc).update(anyString(), eq(5L), eq("role-uuid-1"));
    }

    @Test
    void removeRole_deletesAllAssignments() {
        adapter.removeRole(5L);

        verify(jdbc).update(anyString(), eq(5L));
    }

    @Test
    void findRoleIdByTenantUserId_returnsEmpty_whenNoRow() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(5L))).thenReturn(List.of());

        assertThat(adapter.findRoleIdByTenantUserId(5L)).isEmpty();
    }

    @Test
    void findRoleIdByTenantUserId_returnsRoleId_whenFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(5L)))
                .thenReturn(List.of("role-uuid-1"));

        assertThat(adapter.findRoleIdByTenantUserId(5L)).contains("role-uuid-1");
    }

    @Test
    void savePerson_insertsAndReturnsLastVal() {
        when(jdbc.queryForObject(eq("SELECT LASTVAL()"), eq(Integer.class))).thenReturn(123);

        Integer id = adapter.savePerson("DNI", "12345678", "Ana", "Perez", "Lopez");

        assertThat(id).isEqualTo(123);
        verify(jdbc).update(anyString(), eq("DNI"), eq("12345678"),
                eq("Ana"), eq("Perez"), eq("Lopez"));
    }

    @Test
    void updatePerson_executesUpdate() {
        adapter.updatePerson(7, "Nuevo", "Apellido", "Apellido2");

        verify(jdbc).update(anyString(), eq("Nuevo"), eq("Apellido"),
                eq("Apellido2"), eq(7));
    }

    @Test
    void findPersonIdByDocument_returnsEmpty_whenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("DNI"), eq("111")))
                .thenReturn(List.of());

        assertThat(adapter.findPersonIdByDocument("DNI", "111")).isEmpty();
    }

    @Test
    void findPersonIdByDocument_returnsId_whenFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("DNI"), eq("111")))
                .thenReturn(List.of(42));

        assertThat(adapter.findPersonIdByDocument("DNI", "111")).contains(42);
    }

    @Test
    void profileRowMapper_mapsAllFields() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        var now = LocalDateTime.now();
        var createdTs = Timestamp.valueOf(now);
        var updatedTs = Timestamp.valueOf(now.plusHours(1));

        when(rs.getTimestamp("created_at")).thenReturn(createdTs);
        when(rs.getTimestamp("updated_at")).thenReturn(updatedTs);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("email")).thenReturn("ana@x.pe");
        when(rs.getString("phone")).thenReturn("999");
        when(rs.getString("state")).thenReturn("ACTIVO");
        when(rs.getBoolean("must_change_password")).thenReturn(true);
        when(rs.getInt("person_id")).thenReturn(11);
        when(rs.getString("document_type")).thenReturn("DNI");
        when(rs.getString("document_value")).thenReturn("12345678");
        when(rs.getString("name")).thenReturn("Ana");
        when(rs.getString("father_surname")).thenReturn("Perez");
        when(rs.getString("mother_surname")).thenReturn("Lopez");
        when(rs.getString("role_id")).thenReturn("role-uuid-1");
        when(rs.getString("role_name")).thenReturn("admin");

        TenantUserPort.TenantUserProfileProjection projection = invokeRowMapper(rs);

        assertThat(projection.id()).isEqualTo(1L);
        assertThat(projection.email()).isEqualTo("ana@x.pe");
        assertThat(projection.estado()).isEqualTo("ACTIVO");
        assertThat(projection.mustChangePassword()).isTrue();
        assertThat(projection.createdAt()).isEqualTo(now);
        assertThat(projection.updatedAt()).isEqualTo(now.plusHours(1));
        assertThat(projection.personId()).isEqualTo(11);
        assertThat(projection.documentType()).isEqualTo("DNI");
        assertThat(projection.roleId()).isEqualTo("role-uuid-1");
    }

    @Test
    void profileRowMapper_handlesNullTimestamps() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("created_at")).thenReturn(null);
        when(rs.getTimestamp("updated_at")).thenReturn(null);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("email")).thenReturn(null);
        when(rs.getString("phone")).thenReturn(null);
        when(rs.getString("state")).thenReturn("ACTIVO");
        when(rs.getBoolean("must_change_password")).thenReturn(false);
        when(rs.getInt("person_id")).thenReturn(0);
        when(rs.getString("document_type")).thenReturn(null);
        when(rs.getString("document_value")).thenReturn(null);
        when(rs.getString("name")).thenReturn(null);
        when(rs.getString("father_surname")).thenReturn(null);
        when(rs.getString("mother_surname")).thenReturn(null);
        when(rs.getString("role_id")).thenReturn(null);
        when(rs.getString("role_name")).thenReturn(null);

        TenantUserPort.TenantUserProfileProjection projection = invokeRowMapper(rs);

        assertThat(projection.createdAt()).isNull();
        assertThat(projection.updatedAt()).isNull();
    }

    @SuppressWarnings("unchecked")
    private TenantUserPort.TenantUserProfileProjection invokeRowMapper(ResultSet rs) throws SQLException {
        var projectionHolder = new java.util.concurrent.atomic.AtomicReference<TenantUserPort.TenantUserProfileProjection>();
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(inv -> {
                    RowMapper<TenantUserPort.TenantUserProfileProjection> mapper = inv.getArgument(1);
                    projectionHolder.set(mapper.mapRow(rs, 0));
                    return List.of(projectionHolder.get());
                });
        adapter.findAllProfiles(null, null, 0, 10, null);
        return projectionHolder.get();
    }

    private TenantUserPort.TenantUserProfileProjection sampleProjection(Long id, String email, String state) {
        return new TenantUserPort.TenantUserProfileProjection(
                id, email, "999999999", state, false,
                LocalDateTime.now(), LocalDateTime.now(),
                11, "DNI", "12345678", "Ana", "Perez", "Lopez",
                "role-uuid-1", "admin");
    }
}
