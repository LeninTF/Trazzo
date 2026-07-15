package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.CreateRoleCommand;
import trazzo.back.organization.application.dto.command.UpdateRoleCommand;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepositoryPort roleRepository;

    @InjectMocks
    private RoleService service;

    private Role sampleRole() {
        return Role.restore("role-1", "Admin", "Administrator", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreateRoleCommand("Admin", "Administrator");
        when(roleRepository.existsByName("Admin")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> {
            Role r = i.getArgument(0);
            return Role.restore("role-1", r.getName(), r.getDescription(),
                    r.getCreatedAt(), r.getUpdatedAt());
        });

        var result = service.create(cmd);

        assertThat(result.name()).isEqualTo("Admin");
    }

    @Test
    void create_shouldThrowWhenDuplicate() {
        var cmd = new CreateRoleCommand("Admin", "Desc");
        when(roleRepository.existsByName("Admin")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_shouldReturnResult() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));

        var result = service.findById("role-1");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Admin");
    }

    @Test
    void findAll_shouldReturnPaginatedResult() {
        when(roleRepository.findAll(null, 0, 10, null)).thenReturn(List.of(sampleRole()));
        when(roleRepository.count(null)).thenReturn(1L);

        var result = service.findAll(null, 0, 10, null);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void update_shouldReturnResult() {
        var cmd = new UpdateRoleCommand("Super Admin", "Updated");
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(roleRepository.existsByNameAndIdNot("Super Admin", "role-1")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.update("role-1", cmd);

        assertThat(result.name()).isEqualTo("Super Admin");
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        var cmd = new UpdateRoleCommand("Name", "Desc");
        when(roleRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("bad-id", cmd))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void update_shouldThrowWhenDuplicateName() {
        var cmd = new UpdateRoleCommand("Taken", "Desc");
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));
        when(roleRepository.existsByNameAndIdNot("Taken", "role-1")).thenReturn(true);

        assertThatThrownBy(() -> service.update("role-1", cmd))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void delete_shouldDelete() {
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(sampleRole()));

        service.delete("role-1");

        verify(roleRepository).deleteById("role-1");
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(roleRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("bad-id"))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
