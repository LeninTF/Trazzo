package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import trazzo.back.corehr.application.dto.command.CreateTenantUserCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserCommand;
import trazzo.back.corehr.application.dto.result.SoftDeleteResult;
import trazzo.back.corehr.application.dto.result.TenantUserProfileResult;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.TenantUserPort.TenantUserProfileProjection;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class TenantUserServiceTest {

    private TenantUserPort tenantUserPort;
    private UserRepositoryPort userRepository;
    private PasswordEncoder passwordEncoder;
    private TenantUserService service;

    private final TenantUserPort.OrgAssignmentBundle EMPTY_ORG = new TenantUserPort.OrgAssignmentBundle(List.of(), List.of(), List.of());

    @BeforeEach
    void setUp() {
        tenantUserPort = mock(TenantUserPort.class);
        userRepository = mock(UserRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new TenantUserService(tenantUserPort, userRepository, passwordEncoder);
        when(tenantUserPort.findOrgAssignmentsByUserIds(any())).thenReturn(Map.of());
    }

    private TenantUserProfileProjection buildProjection(Long id) {
        return new TenantUserProfileProjection(
                id, "user@test.com", "999999999", "ACTIVO", false,
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 6, 1, 12, 0),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez",
                "role-1", "Admin"
        );
    }

    private TenantUserProfileProjection buildProjectionWithoutRole(Long id) {
        return new TenantUserProfileProjection(
                id, "user@test.com", "999999999", "ACTIVO", false,
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 6, 1, 12, 0),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez",
                null, null
        );
    }

    // ── findAll ────────────────────────────────────────────────────────

    @Test
    void findAllReturnsEmptyResult() {
        when(tenantUserPort.findAllProfiles(null, null, 0, 10, null)).thenReturn(List.of());
        when(tenantUserPort.countAllProfiles(null, null)).thenReturn(0L);

        var result = service.findAll(null, null, 0, 10, null);

        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var p1 = buildProjection(1L);
        var p2 = buildProjection(2L);
        when(tenantUserPort.findAllProfiles(null, null, 0, 10, null)).thenReturn(List.of(p1, p2));
        when(tenantUserPort.countAllProfiles(null, null)).thenReturn(2L);

        var result = service.findAll(null, null, 0, 10, null);

        assertEquals(2, result.content().size());
        assertEquals("user@test.com", result.content().get(0).email());
        assertEquals(1L, result.content().get(0).id());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void findAllWithSearchFilter() {
        var p = buildProjection(1L);
        when(tenantUserPort.findAllProfiles("Juan", null, 0, 10, null)).thenReturn(List.of(p));
        when(tenantUserPort.countAllProfiles("Juan", null)).thenReturn(1L);

        var result = service.findAll("Juan", null, 0, 10, null);

        assertEquals(1, result.content().size());
        verify(tenantUserPort).findAllProfiles("Juan", null, 0, 10, null);
        verify(tenantUserPort).countAllProfiles("Juan", null);
    }

    @Test
    void findAllWithStatusFilter() {
        var p = buildProjection(1L);
        when(tenantUserPort.findAllProfiles(null, "ACTIVO", 0, 10, null)).thenReturn(List.of(p));
        when(tenantUserPort.countAllProfiles(null, "ACTIVO")).thenReturn(1L);

        var result = service.findAll(null, "ACTIVO", 0, 10, null);

        assertEquals(1, result.content().size());
        verify(tenantUserPort).findAllProfiles(null, "ACTIVO", 0, 10, null);
    }

    @Test
    void findAllWithZeroSizeReturnsZeroTotalPages() {
        when(tenantUserPort.findAllProfiles(null, null, 0, 0, null)).thenReturn(List.of());
        when(tenantUserPort.countAllProfiles(null, null)).thenReturn(5L);

        var result = service.findAll(null, null, 0, 0, null);

        assertEquals(0, result.totalPages());
    }

    // ── findById ───────────────────────────────────────────────────────

    @Test
    void findByIdReturnsResultWhenFound() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(projection));

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        assertEquals("Juan", result.get().persona().name());
        assertEquals("Admin", result.get().rol().name());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(tenantUserPort.findProfileById(99L)).thenReturn(Optional.empty());

        var result = service.findById(99L);

        assertTrue(result.isEmpty());
    }

    // ── create ─────────────────────────────────────────────────────────

    @Test
    void createWithNewPerson() {
        var command = new CreateTenantUserCommand("DNI", "12345678", "Juan", "Perez", "Lopez",
                "2000-01-01", null, "juan@test.com", "999999999", "role-1",
                null, null, null);
        var savedUserId = UUID.randomUUID();

        when(tenantUserPort.findPersonIdByDocument("DNI", "12345678")).thenReturn(Optional.empty());
        when(tenantUserPort.savePerson("DNI", "12345678", "Juan", "Perez", "Lopez")).thenReturn(1);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pw");
        when(userRepository.save(any())).thenAnswer(inv -> inv.<User>getArgument(0));
        when(tenantUserPort.saveTenantUser(any(UUID.class))).thenReturn(10L);
        when(tenantUserPort.findProfileById(10L)).thenReturn(Optional.of(buildProjection(10L)));

        var result = service.create(command);

        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(tenantUserPort).savePerson("DNI", "12345678", "Juan", "Perez", "Lopez");
        verify(tenantUserPort).saveTenantUser(any(UUID.class));
        verify(tenantUserPort).assignRole(10L, "role-1");
    }

    @Test
    void createWithExistingPerson() {
        var command = new CreateTenantUserCommand("DNI", "12345678", "Juan", "Perez", "Lopez",
                "2000-01-01", null, "juan@test.com", "999999999", null,
                null, null, null);

        when(tenantUserPort.findPersonIdByDocument("DNI", "12345678")).thenReturn(Optional.of(5));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pw");
        when(userRepository.save(any())).thenAnswer(inv -> inv.<User>getArgument(0));
        when(tenantUserPort.saveTenantUser(any(UUID.class))).thenReturn(10L);
        when(tenantUserPort.findProfileById(10L)).thenReturn(Optional.of(buildProjectionWithoutRole(10L)));

        var result = service.create(command);

        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(tenantUserPort, never()).savePerson(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(tenantUserPort, never()).assignRole(anyLong(), anyString());
    }

    @Test
    void createWithoutRoleDoesNotAssignRole() {
        var command = new CreateTenantUserCommand("DNI", "12345678", "Juan", "Perez", "Lopez",
                "2000-01-01", null, "juan@test.com", "999999999", null,
                null, null, null);

        when(tenantUserPort.findPersonIdByDocument("DNI", "12345678")).thenReturn(Optional.of(5));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pw");
        when(userRepository.save(any())).thenAnswer(inv -> inv.<User>getArgument(0));
        when(tenantUserPort.saveTenantUser(any(UUID.class))).thenReturn(10L);
        when(tenantUserPort.findProfileById(10L)).thenReturn(Optional.of(buildProjectionWithoutRole(10L)));

        var result = service.create(command);

        verify(tenantUserPort, never()).assignRole(anyLong(), anyString());
        assertNotNull(result);
    }

    @Test
    void createThrowsWhenRetrievalFails() {
        var command = new CreateTenantUserCommand("DNI", "12345678", "Juan", "Perez", "Lopez",
                "2000-01-01", null, "juan@test.com", "999999999", null,
                null, null, null);

        when(tenantUserPort.findPersonIdByDocument("DNI", "12345678")).thenReturn(Optional.empty());
        when(tenantUserPort.savePerson("DNI", "12345678", "Juan", "Perez", "Lopez")).thenReturn(1);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pw");
        when(userRepository.save(any())).thenAnswer(inv -> inv.<User>getArgument(0));
        when(tenantUserPort.saveTenantUser(any(UUID.class))).thenReturn(10L);
        when(tenantUserPort.findProfileById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.create(command));
    }

    // ── update ─────────────────────────────────────────────────────────

    @Test
    void updateSuccess() {
        var command = new CreateTenantUserCommand("DNI", "87654321", "Maria", "Garcia", "Lopez",
                "1990-05-10", null, "maria@test.com", "888888888", "role-2",
                null, null, null);

        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(buildProjection(1L)),
                Optional.of(buildProjection(1L))
        );

        var result = service.update(1L, command);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(tenantUserPort).removeRole(1L);
        verify(tenantUserPort).assignRole(1L, "role-2");
    }

    @Test
    void updateWhenExistingHasNoRoleDoesNotCallRemoveRole() {
        var command = new CreateTenantUserCommand("DNI", "87654321", "Maria", "Garcia", "Lopez",
                "1990-05-10", null, "maria@test.com", "888888888", "role-2",
                null, null, null);

        var projectionNoRole = buildProjectionWithoutRole(1L);
        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(projectionNoRole),
                Optional.of(projectionNoRole)
        );

        service.update(1L, command);

        verify(tenantUserPort, never()).removeRole(anyLong());
        verify(tenantUserPort).assignRole(1L, "role-2");
    }

    @Test
    void updateThrowsWhenNotFound() {
        var command = new CreateTenantUserCommand("DNI", "87654321", "Maria", "Garcia", "Lopez",
                "1990-05-10", null, "maria@test.com", "888888888", null,
                null, null, null);

        when(tenantUserPort.findProfileById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(99L, command));
    }

    // ── patch ──────────────────────────────────────────────────────────

    @Test
    void patchUpdatesName() {
        var command = new PatchTenantUserCommand("Maria", null, "Lopez", null, null, null, null, null, null, null, null, null, null);
        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(buildProjection(1L)),
                Optional.of(buildProjection(1L))
        );

        service.patch(1L, command);

        verify(tenantUserPort).updatePerson(1, "Maria", "Perez", "Lopez");
    }

    @Test
    void patchUpdatesEstado() {
        var command = new PatchTenantUserCommand(null, null, null, null, null, null, null, null, "LICENCIA", null, null, null, null);
        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(buildProjection(1L)),
                Optional.of(buildProjection(1L))
        );

        service.patch(1L, command);

        verify(tenantUserPort).updateState(1L, trazzo.back.corehr.domain.model.TenantUserState.LICENCIA);
    }

    @Test
    void patchUpdatesRole() {
        var command = new PatchTenantUserCommand(null, null, null, null, null, null, null, null, null, "role-new", null, null, null);
        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(buildProjection(1L)),
                Optional.of(buildProjection(1L))
        );

        service.patch(1L, command);

        verify(tenantUserPort).assignRole(1L, "role-new");
    }

    @Test
    void patchWithNullFieldsDoesNotCallUpdatePerson() {
        var command = new PatchTenantUserCommand(null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(buildProjection(1L)),
                Optional.of(buildProjection(1L))
        );

        service.patch(1L, command);

        verify(tenantUserPort, never()).updatePerson(anyInt(), anyString(), anyString(), anyString());
        verify(tenantUserPort, never()).updateState(anyLong(), any());
        verify(tenantUserPort, never()).assignRole(anyLong(), anyString());
    }

    @Test
    void patchThrowsWhenNotFound() {
        var command = new PatchTenantUserCommand("X", null, null, null, null, null, null, null, null, null, null, null, null);
        when(tenantUserPort.findProfileById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, command));
    }

    // ── delete ─────────────────────────────────────────────────────────

    @Test
    void deleteSuccess() {
        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(buildProjection(1L)));

        SoftDeleteResult result = service.delete(1L);

        assertEquals(1L, result.id());
        assertEquals("INACTIVO", result.status());
        assertNotNull(result.deletedAt());
        verify(tenantUserPort).softDelete(1L);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(tenantUserPort.findProfileById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(99L));
        verify(tenantUserPort, never()).softDelete(anyLong());
    }

    // ── assignRole ─────────────────────────────────────────────────────

    @Test
    void assignRoleSuccess() {
        when(tenantUserPort.findProfileById(1L)).thenReturn(
                Optional.of(buildProjection(1L)),
                Optional.of(buildProjection(1L))
        );

        var result = service.assignRole(1L, "role-2");

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(tenantUserPort).assignRole(1L, "role-2");
    }

    @Test
    void assignRoleThrowsWhenNotFound() {
        when(tenantUserPort.findProfileById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.assignRole(99L, "role-2"));
        verify(tenantUserPort, never()).assignRole(anyLong(), anyString());
    }

    // ── changePassword ─────────────────────────────────────────────────

    @Test
    void changePasswordSuccess() {
        var projection = buildProjection(1L);
        var user = User.restore("uuid-1", 1, null, "user@test.com", "999999999",
                "old-encoded", List.of(), List.of(), false,
                LocalDateTime.now(), LocalDateTime.now(), null);

        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(projection));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("new-encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.<User>getArgument(0));

        assertDoesNotThrow(() -> service.changePassword(1L, "oldPass", "newPass"));
        verify(userRepository).save(any());
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordIsWrong() {
        var projection = buildProjection(1L);
        var user = User.restore("uuid-1", 1, null, "user@test.com", "999999999",
                "old-encoded", List.of(), List.of(), false,
                LocalDateTime.now(), LocalDateTime.now(), null);

        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(projection));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "old-encoded")).thenReturn(false);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.changePassword(1L, "wrongPass", "newPass"));
        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordThrowsWhenNewPasswordIsBlank() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(projection));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.changePassword(1L, "old", "  "));
        assertEquals("New password is required", ex.getMessage());
    }

    @Test
    void changePasswordThrowsWhenNewPasswordIsNull() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(projection));

        var ex = assertThrows(IllegalArgumentException.class, () -> service.changePassword(1L, "old", null));
        assertEquals("New password is required", ex.getMessage());
    }

    @Test
    void changePasswordThrowsWhenTenantUserNotFound() {
        when(tenantUserPort.findProfileById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.changePassword(99L, "old", "new"));
    }

    @Test
    void changePasswordThrowsWhenMasterUserNotFound() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findProfileById(1L)).thenReturn(Optional.of(projection));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.changePassword(1L, "old", "new"));
    }

    // ── findMe ─────────────────────────────────────────────────────────

    @Test
    void findMeReturnsResultWhenFound() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null)).thenReturn(List.of(projection));

        var masterUser = User.restore("master-1", 1, null, "user@test.com", "999999999",
                "pw", List.of(), List.of(), false,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(masterUser));

        var result = service.findMe("master-1");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
    }

    @Test
    void findMeReturnsEmptyWhenNoProfiles() {
        when(tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null)).thenReturn(List.of());

        var result = service.findMe("master-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void findMeReturnsEmptyWhenEmailDoesNotMatchMasterUser() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null)).thenReturn(List.of(projection));

        var masterUser = User.restore("other-master", 1, null, "user@test.com", "999999999",
                "pw", List.of(), List.of(), false,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(masterUser));

        var result = service.findMe("master-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void findMeSkipsProjectionsWithNullEmail() {
        var projectionNoEmail = new TenantUserPort.TenantUserProfileProjection(
                1L, null, "999999999", "ACTIVO", false,
                LocalDateTime.now(), LocalDateTime.now(),
                1, "DNI", "12345678", "Juan", "Perez", "Lopez",
                null, null
        );
        when(tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null)).thenReturn(List.of(projectionNoEmail));

        var result = service.findMe("master-1");

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByEmail(anyString());
    }

    // ── patchMe ────────────────────────────────────────────────────────

    @Test
    void patchMeReturnsResult() {
        var projection = buildProjection(1L);
        when(tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null)).thenReturn(List.of(projection));

        var masterUser = User.restore("master-1", 1, null, "user@test.com", "999999999",
                "pw", List.of(), List.of(), false,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(masterUser));

        var result = service.patchMe("master-1", "111111111", "img.jpg");

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("user@test.com", result.email());
    }

    @Test
    void patchMeThrowsWhenNotFound() {
        when(tenantUserPort.findAllProfiles(null, null, 0, Integer.MAX_VALUE, null)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.patchMe("nonexistent", "111111111", "img.jpg"));
    }
}
