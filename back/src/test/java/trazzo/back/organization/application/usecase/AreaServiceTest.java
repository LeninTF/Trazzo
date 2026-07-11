package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.CreateAreaCommand;
import trazzo.back.organization.application.dto.command.UpdateAreaCommand;
import trazzo.back.organization.application.port.out.AreaRepositoryPort;
import trazzo.back.organization.application.port.out.BranchRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.business.Area;
import trazzo.back.organization.domain.model.business.Branch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock AreaRepositoryPort areaRepository;
    @Mock BranchRepositoryPort branchRepository;
    @InjectMocks AreaService service;

    private Branch stubBranch(Long id) {
        return Branch.restore(id, "Branch", "desc", true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Area stubArea(Long id, Long branchId, String name) {
        return Area.restore(id, branchId, name, "desc", true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void create_happyPath_savesAndReturns() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(stubBranch(1L)));
        when(areaRepository.existsByBranchIdAndName(1L, "Sales")).thenReturn(false);
        when(areaRepository.save(any())).thenReturn(stubArea(10L, 1L, "Sales"));

        var result = service.create(new CreateAreaCommand(1L, "Sales", "desc"));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Sales");
    }

    @Test
    void create_branchNotFound_throwsOrgNotFoundException() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateAreaCommand(99L, "Sales", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void create_branchSoftDeleted_throwsOrgNotFoundException() {
        var deleted = Branch.restore(1L, "Branch", "desc", false, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.create(new CreateAreaCommand(1L, "Sales", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void create_duplicateName_throwsDuplicateOrgNameException() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(stubBranch(1L)));
        when(areaRepository.existsByBranchIdAndName(1L, "Sales")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateAreaCommand(1L, "Sales", "d")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_found_returnsResult() {
        when(areaRepository.findById(5L)).thenReturn(Optional.of(stubArea(5L, 1L, "IT")));

        var result = service.findById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("IT");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedResult() {
        when(areaRepository.findAll(null, null, null, 0, 5, null))
                .thenReturn(List.of(stubArea(1L, 1L, "A")));
        when(areaRepository.count(null, null, null)).thenReturn(1L);

        var result = service.findAll(null, null, null, 0, 5, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.total()).isEqualTo(1L);
    }

    @Test
    void update_happyPath_returnsUpdatedResult() {
        var area = stubArea(1L, 1L, "Old");
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.existsByBranchIdAndNameAndIdNot(1L, "New", 1L)).thenReturn(false);
        when(areaRepository.save(any())).thenReturn(stubArea(1L, 1L, "New"));

        var result = service.update(1L, new UpdateAreaCommand("New", "d"));

        assertThat(result.name()).isEqualTo("New");
    }

    @Test
    void update_notFound_throwsOrgNotFoundException() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateAreaCommand("X", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void delete_happyPath_softDeletesAndSaves() {
        var area = stubArea(1L, 1L, "Sales");
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        service.delete(1L);

        verify(areaRepository).save(area);
    }

    @Test
    void delete_notFound_throwsOrgNotFoundException() {
        when(areaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
