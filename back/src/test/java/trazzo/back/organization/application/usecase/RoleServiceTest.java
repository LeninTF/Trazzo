package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.CreateRoleCommand;
import trazzo.back.organization.application.dto.command.UpdateRoleCommand;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleRepositoryPort roleRepository;
    @InjectMocks RoleService service;

    private Role stubRole(String id, String name) {
        return Role.restore(id, name, "desc", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_happyPath_savesAndReturns() {
        when(roleRepository.existsByName("Admin")).thenReturn(false);
        when(roleRepository.save(any())).thenReturn(stubRole("uuid-1", "Admin"));

        var result = service.create(new CreateRoleCommand("Admin", "desc"));

        assertThat(result.id()).isEqualTo("uuid-1");
        assertThat(result.name()).isEqualTo("Admin");
    }

    @Test
    void create_duplicateName_throwsDuplicateOrgNameException() {
        when(roleRepository.existsByName("Admin")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateRoleCommand("Admin", "d")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_found_returnsResult() {
        when(roleRepository.findById("uuid-1")).thenReturn(Optional.of(stubRole("uuid-1", "Viewer")));

        var result = service.findById("uuid-1");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Viewer");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThat(service.findById("x")).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedResult() {
        when(roleRepository.findAll(null, 0, 10, null))
                .thenReturn(List.of(stubRole("a", "A"), stubRole("b", "B")));
        when(roleRepository.count(null)).thenReturn(2L);

        var result = service.findAll(null, 0, 10, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.total()).isEqualTo(2L);
    }

    @Test
    void update_happyPath_returnsUpdatedResult() {
        when(roleRepository.findById("uuid-1")).thenReturn(Optional.of(stubRole("uuid-1", "Old")));
        when(roleRepository.existsByNameAndIdNot("New", "uuid-1")).thenReturn(false);
        when(roleRepository.save(any())).thenReturn(stubRole("uuid-1", "New"));

        var result = service.update("uuid-1", new UpdateRoleCommand("New", "d"));

        assertThat(result.name()).isEqualTo("New");
    }

    @Test
    void update_notFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("x", new UpdateRoleCommand("Y", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void update_duplicateName_throwsDuplicateOrgNameException() {
        when(roleRepository.findById("uuid-1")).thenReturn(Optional.of(stubRole("uuid-1", "Old")));
        when(roleRepository.existsByNameAndIdNot("Taken", "uuid-1")).thenReturn(true);

        assertThatThrownBy(() -> service.update("uuid-1", new UpdateRoleCommand("Taken", "d")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void delete_happyPath_deletesById() {
        when(roleRepository.findById("uuid-1")).thenReturn(Optional.of(stubRole("uuid-1", "Admin")));

        service.delete("uuid-1");

        verify(roleRepository).deleteById("uuid-1");
    }

    @Test
    void delete_notFound_throwsOrgNotFoundException() {
        when(roleRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("x"))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
