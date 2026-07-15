package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.CreatePermissionCommand;
import trazzo.back.organization.application.dto.command.UpdatePermissionCommand;
import trazzo.back.organization.application.port.out.PermissionRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Permissions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepositoryPort permissionRepository;

    @InjectMocks
    private PermissionService service;

    private Permissions samplePermission() {
        return Permissions.restore("perm-1", "READ", "Read access", "READ_ALL",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreatePermissionCommand("READ", "Read access", "READ_ALL");
        when(permissionRepository.existsByName("READ")).thenReturn(false);
        when(permissionRepository.save(any(Permissions.class))).thenAnswer(i -> {
            Permissions p = i.getArgument(0);
            return Permissions.restore("perm-1", p.getName(), p.getDescription(),
                    p.getMasterFeaturesCode(), p.getCreatedAt(), p.getUpdatedAt());
        });

        var result = service.create(cmd);

        assertThat(result.name()).isEqualTo("READ");
    }

    @Test
    void create_shouldThrowWhenDuplicate() {
        var cmd = new CreatePermissionCommand("READ", "Desc", "CODE");
        when(permissionRepository.existsByName("READ")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_shouldReturnResult() {
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(samplePermission()));

        var result = service.findById("perm-1");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("READ");
    }

    @Test
    void findAll_shouldReturnPaginatedResult() {
        when(permissionRepository.findAll(null, 0, 10, null)).thenReturn(List.of(samplePermission()));
        when(permissionRepository.count(null)).thenReturn(1L);

        var result = service.findAll(null, 0, 10, null);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void update_shouldReturnResult() {
        var cmd = new UpdatePermissionCommand("WRITE", "Write access", "WRITE_ALL");
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(samplePermission()));
        when(permissionRepository.existsByNameAndIdNot("WRITE", "perm-1")).thenReturn(false);
        when(permissionRepository.save(any(Permissions.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.update("perm-1", cmd);

        assertThat(result.name()).isEqualTo("WRITE");
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        var cmd = new UpdatePermissionCommand("Name", "Desc", "Code");
        when(permissionRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("bad-id", cmd))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void delete_shouldDelete() {
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(samplePermission()));

        service.delete("perm-1");

        verify(permissionRepository).deleteById("perm-1");
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(permissionRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("bad-id"))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
