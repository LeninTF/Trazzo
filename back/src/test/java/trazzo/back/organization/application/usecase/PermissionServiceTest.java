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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock PermissionRepositoryPort permissionRepository;
    @InjectMocks PermissionService service;

    private Permissions stubPerm(String id, String name) {
        return Permissions.restore(id, name, "desc", "CODE", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_happyPath_savesAndReturns() {
        when(permissionRepository.existsByName("READ")).thenReturn(false);
        when(permissionRepository.save(any())).thenReturn(stubPerm("uuid-p1", "READ"));

        var result = service.create(new CreatePermissionCommand("READ", "desc", "CODE"));

        assertThat(result.id()).isEqualTo("uuid-p1");
        assertThat(result.name()).isEqualTo("READ");
    }

    @Test
    void create_duplicateName_throwsDuplicateOrgNameException() {
        when(permissionRepository.existsByName("READ")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreatePermissionCommand("READ", "d", "C")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_found_returnsResult() {
        when(permissionRepository.findById("uuid-p1")).thenReturn(Optional.of(stubPerm("uuid-p1", "WRITE")));

        var result = service.findById("uuid-p1");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("WRITE");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(permissionRepository.findById("x")).thenReturn(Optional.empty());

        assertThat(service.findById("x")).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedResult() {
        when(permissionRepository.findAll(null, 0, 20, null))
                .thenReturn(List.of(stubPerm("a", "A"), stubPerm("b", "B")));
        when(permissionRepository.count(null)).thenReturn(2L);

        var result = service.findAll(null, 0, 20, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.total()).isEqualTo(2L);
    }

    @Test
    void update_happyPath_returnsUpdatedResult() {
        when(permissionRepository.findById("uuid-p1")).thenReturn(Optional.of(stubPerm("uuid-p1", "OLD")));
        when(permissionRepository.existsByNameAndIdNot("NEW", "uuid-p1")).thenReturn(false);
        when(permissionRepository.save(any())).thenReturn(stubPerm("uuid-p1", "NEW"));

        var result = service.update("uuid-p1", new UpdatePermissionCommand("NEW", "d", "C"));

        assertThat(result.name()).isEqualTo("NEW");
    }

    @Test
    void update_notFound_throwsOrgNotFoundException() {
        when(permissionRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("x", new UpdatePermissionCommand("Y", "d", "C")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void delete_happyPath_deletesById() {
        when(permissionRepository.findById("uuid-p1")).thenReturn(Optional.of(stubPerm("uuid-p1", "READ")));

        service.delete("uuid-p1");

        verify(permissionRepository).deleteById("uuid-p1");
    }

    @Test
    void delete_notFound_throwsOrgNotFoundException() {
        when(permissionRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("x"))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
