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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleRepositoryPort userRoleRepository;

    @Mock
    private RoleRepositoryPort roleRepository;

    @InjectMocks
    private UserRoleService service;

    private Role sampleRole() {
        return Role.restore("role-1", "Admin", "Administrator", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void assign_shouldReturnResult() {
        var cmd = new AssignRoleToUserCommand("role-1", 1L);
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(userRoleRepository.existsByTenantUserIdAndRoleIdAndDepartmentId(1L, "role-1", 1L)).thenReturn(false);
        when(userRoleRepository.save(any(TenantUserRole.class))).thenAnswer(i -> {
            TenantUserRole tur = i.getArgument(0);
            return TenantUserRole.restore(1L, tur.getTenantUserId(), tur.getRoleId(),
                    tur.getDepartmentId(), tur.getCreatedAt());
        });

        var result = service.assign(1L, cmd);

        assertThat(result.roleId()).isEqualTo("role-1");
    }

    @Test
    void assign_shouldThrowWhenRoleNotFound() {
        var cmd = new AssignRoleToUserCommand("bad-role", 1L);
        when(roleRepository.findById("bad-role")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(1L, cmd))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void assign_shouldThrowWhenDuplicate() {
        var cmd = new AssignRoleToUserCommand("role-1", 1L);
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(userRoleRepository.existsByTenantUserIdAndRoleIdAndDepartmentId(1L, "role-1", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.assign(1L, cmd))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findByTenantUserId_shouldReturnList() {
        var assignment = TenantUserRole.restore(1L, 1L, "role-1", 1L, LocalDateTime.now());
        when(userRoleRepository.findByTenantUserId(1L)).thenReturn(List.of(assignment));

        var result = service.findByTenantUserId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByRoleId_shouldReturnList() {
        var assignment = TenantUserRole.restore(1L, 1L, "role-1", 1L, LocalDateTime.now());
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(userRoleRepository.findByRoleId("role-1")).thenReturn(List.of(assignment));

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
        var assignment = TenantUserRole.restore(1L, 1L, "role-1", 1L, LocalDateTime.now());
        when(userRoleRepository.findById(1L)).thenReturn(Optional.of(assignment));

        service.remove(1L, 1L);

        verify(userRoleRepository).deleteById(1L);
    }

    @Test
    void remove_shouldThrowWhenNotFound() {
        when(userRoleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(1L, 999L))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void remove_shouldThrowWhenUserMismatch() {
        var assignment = TenantUserRole.restore(1L, 2L, "role-1", 1L, LocalDateTime.now());
        when(userRoleRepository.findById(1L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.remove(1L, 1L))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
