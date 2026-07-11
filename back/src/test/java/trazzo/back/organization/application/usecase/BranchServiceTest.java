package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.CreateBranchCommand;
import trazzo.back.organization.application.dto.command.UpdateBranchCommand;
import trazzo.back.organization.application.port.out.BranchRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.business.Branch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock BranchRepositoryPort branchRepository;
    @InjectMocks BranchService service;

    private Branch stubBranch(Long id, String name) {
        return Branch.restore(id, name, "desc", true,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void create_savesAndReturnsResult() {
        var cmd = new CreateBranchCommand("Main", "Main branch");
        var saved = stubBranch(1L, "Main");
        when(branchRepository.existsByName("Main")).thenReturn(false);
        when(branchRepository.save(any())).thenReturn(saved);

        var result = service.create(cmd);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Main");
    }

    @Test
    void create_duplicateName_throwsDuplicateOrgNameException() {
        when(branchRepository.existsByName("Main")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateBranchCommand("Main", "desc")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_found_returnsResult() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(stubBranch(1L, "HQ")));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("HQ");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedResult() {
        when(branchRepository.findAll(null, null, 0, 10, null))
                .thenReturn(List.of(stubBranch(1L, "A"), stubBranch(2L, "B")));
        when(branchRepository.count(null, null)).thenReturn(2L);

        var result = service.findAll(null, null, 0, 10, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.total()).isEqualTo(2L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void update_happyPath_returnsUpdatedResult() {
        var branch = stubBranch(1L, "Old");
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchRepository.existsByNameAndIdNot("New", 1L)).thenReturn(false);
        when(branchRepository.save(any())).thenReturn(stubBranch(1L, "New"));

        var result = service.update(1L, new UpdateBranchCommand("New", "desc"));

        assertThat(result.name()).isEqualTo("New");
    }

    @Test
    void update_notFound_throwsOrgNotFoundException() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateBranchCommand("X", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void update_duplicateName_throwsDuplicateOrgNameException() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(stubBranch(1L, "Old")));
        when(branchRepository.existsByNameAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new UpdateBranchCommand("Taken", "d")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void delete_happyPath_softDeletesAndSaves() {
        var branch = stubBranch(1L, "Main");
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

        service.delete(1L);

        verify(branchRepository).save(branch);
    }

    @Test
    void delete_notFound_throwsOrgNotFoundException() {
        when(branchRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
