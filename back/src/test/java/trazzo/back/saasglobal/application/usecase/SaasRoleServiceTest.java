package trazzo.back.saasglobal.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.saasglobal.application.dto.command.CreateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRoleCommand;
import trazzo.back.saasglobal.application.dto.command.UpdateRolePermissionsCommand;
import trazzo.back.saasglobal.application.dto.result.SaasRoleResult;
import trazzo.back.saasglobal.application.port.out.RoleMasterRepositoryPort;
import trazzo.back.saasglobal.domain.exception.RoleInUseException;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

@ExtendWith(MockitoExtension.class)
class SaasRoleServiceTest {

    @Mock RoleMasterRepositoryPort roleRepository;
    @InjectMocks SaasRoleService service;

    private static RoleMaster role(int id, String name) {
        return RoleMaster.restore(id, name, "Display " + name, "desc", List.of("monitoreo-sistema.dashboard-global"));
    }

    @Test
    void listAll_marksAdminTrazzoAsSystemManaged() {
        when(roleRepository.findAll()).thenReturn(List.of(role(1, "admin_trazzo"), role(2, "soporte")));

        List<SaasRoleResult> results = service.listAll();

        assertTrue(results.get(0).systemManaged());
        assertFalse(results.get(1).systemManaged());
    }

    @Test
    void getById_returnsResult() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(role(2, "soporte")));

        SaasRoleResult result = service.getById(2);

        assertEquals("soporte", result.name());
        assertEquals(List.of("monitoreo-sistema.dashboard-global"), result.permissions());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(99));
    }

    @Test
    void create_savesAndReturnsResult() {
        when(roleRepository.save(any())).thenReturn(role(3, "financiero"));
        var command = new CreateRoleCommand("financiero", "Financiero", "desc");

        SaasRoleResult result = service.create(command);

        assertEquals("financiero", result.name());
    }

    @Test
    void update_savesChanges() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(role(2, "soporte")));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var command = new UpdateRoleCommand(2, "soporte-v2", "Soporte V2", "nueva desc");

        SaasRoleResult result = service.update(command);

        assertEquals("soporte-v2", result.name());
    }

    @Test
    void update_throwsWhenRoleIsAdminTrazzo() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(role(1, "admin_trazzo")));
        var command = new UpdateRoleCommand(1, "x", "X", null);

        assertThrows(RoleInUseException.class, () -> service.update(command));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void deleteById_throwsWhenRoleIsAdminTrazzo() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(role(1, "admin_trazzo")));

        assertThrows(RoleInUseException.class, () -> service.deleteById(1));
        verify(roleRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_throwsWhenRoleAssignedToUsers() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(role(2, "soporte")));
        when(roleRepository.isAssignedToAnyUser(2)).thenReturn(true);

        assertThrows(RoleInUseException.class, () -> service.deleteById(2));
        verify(roleRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_deletesWhenUnassigned() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(role(2, "soporte")));
        when(roleRepository.isAssignedToAnyUser(2)).thenReturn(false);

        assertDoesNotThrow(() -> service.deleteById(2));
        verify(roleRepository).deleteById(2);
    }

    @Test
    void updatePermissions_replacesAndReturnsUpdatedRole() {
        when(roleRepository.findById(2))
                .thenReturn(Optional.of(role(2, "soporte")))
                .thenReturn(Optional.of(RoleMaster.restore(2, "soporte", "Soporte", "desc",
                        List.of("gestion-tenants.crear"))));
        var command = new UpdateRolePermissionsCommand(2, List.of("gestion-tenants.crear"));

        SaasRoleResult result = service.updatePermissions(command);

        verify(roleRepository).replacePermissions(2, List.of("gestion-tenants.crear"));
        assertEquals(List.of("gestion-tenants.crear"), result.permissions());
    }

    @Test
    void updatePermissions_throwsOnUnknownCode() {
        when(roleRepository.findById(2)).thenReturn(Optional.of(role(2, "soporte")));
        var command = new UpdateRolePermissionsCommand(2, List.of("no-existe.accion"));

        assertThrows(RuntimeException.class, () -> service.updatePermissions(command));
        verify(roleRepository, never()).replacePermissions(any(), any());
    }

    @Test
    void update_throwsWhenRoleNotFound() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());
        var command = new UpdateRoleCommand(99, "x", "X", null);

        assertThrows(IllegalArgumentException.class, () -> service.update(command));
    }

    @Test
    void deleteById_throwsWhenRoleNotFound() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99));
    }

    @Test
    void listAll_returnsEmptyList() {
        when(roleRepository.findAll()).thenReturn(List.of());

        List<SaasRoleResult> results = service.listAll();

        assertTrue(results.isEmpty());
    }

    @Test
    void create_savesWithCorrectFields() {
        when(roleRepository.save(any())).thenAnswer(inv -> {
            var r = inv.getArgument(0, RoleMaster.class);
            return RoleMaster.restore(5, r.getName(), r.getDisplayName(), r.getDescription(), r.getPermissionCodes());
        });
        var command = new CreateRoleCommand("nuevo", "Nuevo Rol", "Descripción nueva");

        SaasRoleResult result = service.create(command);

        assertEquals("nuevo", result.name());
        assertEquals("Nuevo Rol", result.displayName());
        assertEquals("Descripción nueva", result.description());
        assertFalse(result.systemManaged());
    }
}
