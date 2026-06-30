package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.AssignPermissionToRoleCommand;
import trazzo.back.organization.application.port.out.PermissionRepositoryPort;
import trazzo.back.organization.application.port.out.RolePermissionsRepositoryPort;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Permissions;
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.domain.model.roles.RolePermissions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionsServiceTest {

    @Mock RolePermissionsRepositoryPort rolePermissionsRepository;
    @Mock RoleRepositoryPort roleRepository;
    @Mock PermissionRepositoryPort permissionRepository;
    @InjectMocks RolePermissionsService service;

    private Role stubRole(String id) {
        return Role.restore(id, "Role", "desc", LocalDateTime.now(), LocalDateTime.now());
    }

    private Permissions stubPerm(String id) {
        return Permissions.restore(id, "Perm", "desc", "CODE", LocalDateTime.now(), LocalDateTime.now());
    }

    private RolePermissions stubRp(String roleId, String permId) {
        return RolePermissions.restore(roleId, permId, LocalDateTime.now());
    }

    @Test
    void assign_happyPath_savesAndReturns() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(stubPerm("perm-1")));
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(false);
        when(rolePermissionsRepository.save(any())).thenReturn(stubRp("role-1", "perm-1"));

        var result = service.assign("role-1", new AssignPermissionToRoleCommand("perm-1"));

        assertThat(result.roleId()).isEqualTo("role-1");
        assertThat(result.permissionId()).isEqualTo("perm-1");
    }

    @Test
    void assign_roleNotFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign("x", new AssignPermissionToRoleCommand("perm-1")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void assign_permissionNotFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(permissionRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign("role-1", new AssignPermissionToRoleCommand("x")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void assign_alreadyAssigned_throwsDuplicateOrgNameException() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(stubPerm("perm-1")));
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(true);

        assertThatThrownBy(() -> service.assign("role-1", new AssignPermissionToRoleCommand("perm-1")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findByRoleId_roleExists_returnsList() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(rolePermissionsRepository.findByRoleId("role-1"))
                .thenReturn(List.of(stubRp("role-1", "perm-1"), stubRp("role-1", "perm-2")));

        var result = service.findByRoleId("role-1");

        assertThat(result).hasSize(2);
    }

    @Test
    void findByRoleId_roleNotFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRoleId("x"))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void remove_happyPath_deletesAssignment() {
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(true);

        service.remove("role-1", "perm-1");

        verify(rolePermissionsRepository).deleteByRoleIdAndPermissionId("role-1", "perm-1");
    }

    @Test
    void remove_notAssigned_throwsOrgNotFoundException() {
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("role-1", "perm-1"))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
