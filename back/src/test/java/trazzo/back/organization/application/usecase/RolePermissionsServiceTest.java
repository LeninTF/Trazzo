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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionsServiceTest {

    @Mock
    private RolePermissionsRepositoryPort rolePermissionsRepository;

    @Mock
    private RoleRepositoryPort roleRepository;

    @Mock
    private PermissionRepositoryPort permissionRepository;

    @InjectMocks
    private RolePermissionsService service;

    private Role sampleRole() {
        return Role.restore("role-1", "Admin", "Desc", LocalDateTime.now(), LocalDateTime.now());
    }

    private Permissions samplePermission() {
        return Permissions.restore("perm-1", "READ", "Read", "CODE", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void assign_shouldReturnResult() {
        var cmd = new AssignPermissionToRoleCommand("perm-1");
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(samplePermission()));
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(false);
        when(rolePermissionsRepository.save(any(RolePermissions.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.assign("role-1", cmd);

        assertThat(result.roleId()).isEqualTo("role-1");
        assertThat(result.permissionId()).isEqualTo("perm-1");
    }

    @Test
    void assign_shouldThrowWhenRoleNotFound() {
        var cmd = new AssignPermissionToRoleCommand("perm-1");
        when(roleRepository.findById("bad-role")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign("bad-role", cmd))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void assign_shouldThrowWhenPermissionNotFound() {
        var cmd = new AssignPermissionToRoleCommand("bad-perm");
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(permissionRepository.findById("bad-perm")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign("role-1", cmd))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void assign_shouldThrowWhenDuplicate() {
        var cmd = new AssignPermissionToRoleCommand("perm-1");
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(samplePermission()));
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(true);

        assertThatThrownBy(() -> service.assign("role-1", cmd))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findByRoleId_shouldReturnList() {
        var rp = RolePermissions.restore("role-1", "perm-1", LocalDateTime.now());
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(rolePermissionsRepository.findByRoleId("role-1")).thenReturn(List.of(rp));

        var result = service.findByRoleId("role-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByRoleId_shouldThrowWhenRoleNotFound() {
        when(roleRepository.findById("bad-role")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRoleId("bad-role"))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void remove_shouldDelete() {
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(true);

        service.remove("role-1", "perm-1");

        verify(rolePermissionsRepository).deleteByRoleIdAndPermissionId("role-1", "perm-1");
    }

    @Test
    void remove_shouldThrowWhenNotAssigned() {
        when(rolePermissionsRepository.existsByRoleIdAndPermissionId("role-1", "perm-1")).thenReturn(false);

        assertThatThrownBy(() -> service.remove("role-1", "perm-1"))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
