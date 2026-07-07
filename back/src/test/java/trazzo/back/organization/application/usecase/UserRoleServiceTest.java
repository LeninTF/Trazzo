package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.AssignRoleToUserCommand;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.application.port.out.UserRoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.domain.model.roles.TenantUserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock UserRoleRepositoryPort userRoleRepository;
    @Mock RoleRepositoryPort roleRepository;
    @InjectMocks UserRoleService service;

    private Role stubRole(String id) {
        return Role.restore(id, "Admin", "desc", LocalDateTime.now(), LocalDateTime.now());
    }

    private TenantUserRole stubAssignment(Long id, Long userId, String roleId) {
        return TenantUserRole.restore(id, userId, roleId, null, LocalDateTime.now());
    }

    @Test
    void assign_happyPath_savesAndReturns() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(userRoleRepository.existsByTenantUserIdAndRoleIdAndDepartmentId(10L, "role-1", null))
                .thenReturn(false);
        when(userRoleRepository.save(any())).thenReturn(stubAssignment(1L, 10L, "role-1"));

        var result = service.assign(10L, new AssignRoleToUserCommand("role-1", null));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.tenantUserId()).isEqualTo(10L);
        assertThat(result.roleId()).isEqualTo("role-1");
    }

    @Test
    void assign_roleNotFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(1L, new AssignRoleToUserCommand("x", null)))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void assign_alreadyAssigned_throwsDuplicateOrgNameException() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(userRoleRepository.existsByTenantUserIdAndRoleIdAndDepartmentId(10L, "role-1", null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.assign(10L, new AssignRoleToUserCommand("role-1", null)))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findByTenantUserId_returnsList() {
        when(userRoleRepository.findByTenantUserId(10L))
                .thenReturn(List.of(stubAssignment(1L, 10L, "role-1"), stubAssignment(2L, 10L, "role-2")));

        var result = service.findByTenantUserId(10L);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByRoleId_roleExists_returnsList() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(stubRole("role-1")));
        when(userRoleRepository.findByRoleId("role-1"))
                .thenReturn(List.of(stubAssignment(1L, 10L, "role-1")));

        var result = service.findByRoleId("role-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void findByRoleId_roleNotFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByRoleId("x"))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void remove_happyPath_deletesAssignment() {
        var assignment = stubAssignment(5L, 10L, "role-1");
        when(userRoleRepository.findById(5L)).thenReturn(Optional.of(assignment));

        service.remove(10L, 5L);

        verify(userRoleRepository).deleteById(5L);
    }

    @Test
    void remove_assignmentNotFound_throwsOrgNotFoundException() {
        when(userRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(10L, 99L))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void remove_wrongUser_throwsOrgNotFoundException() {
        var assignment = stubAssignment(5L, 99L, "role-1");
        when(userRoleRepository.findById(5L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.remove(10L, 5L))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
