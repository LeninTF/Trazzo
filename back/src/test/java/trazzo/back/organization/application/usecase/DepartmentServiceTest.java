package trazzo.back.organization.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.application.dto.command.CreateDepartmentCommand;
import trazzo.back.organization.application.dto.command.UpdateDepartmentCommand;
import trazzo.back.organization.application.port.out.AreaRepositoryPort;
import trazzo.back.organization.application.port.out.DepartmentRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.business.Area;
import trazzo.back.organization.domain.model.business.Department;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepositoryPort departmentRepository;
    @Mock AreaRepositoryPort areaRepository;
    @InjectMocks DepartmentService service;

    private Area stubArea(Long id) {
        return Area.restore(id, 1L, "Area", "desc", true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Department stubDept(Long id, Long areaId, String name) {
        return Department.restore(id, areaId, name, "desc", true,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void create_happyPath_savesAndReturns() {
        when(areaRepository.findById(2L)).thenReturn(Optional.of(stubArea(2L)));
        when(departmentRepository.existsByAreaIdAndName(2L, "HR")).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(stubDept(5L, 2L, "HR"));

        var result = service.create(new CreateDepartmentCommand(2L, "HR", "desc"));

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.name()).isEqualTo("HR");
    }

    @Test
    void create_areaNotFound_throwsOrgNotFoundException() {
        when(areaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateDepartmentCommand(99L, "HR", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void create_duplicateName_throwsDuplicateOrgNameException() {
        when(areaRepository.findById(2L)).thenReturn(Optional.of(stubArea(2L)));
        when(departmentRepository.existsByAreaIdAndName(2L, "HR")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateDepartmentCommand(2L, "HR", "d")))
                .isInstanceOf(DuplicateOrgNameException.class);
    }

    @Test
    void findById_found_returnsResult() {
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(stubDept(5L, 2L, "Finance")));

        var result = service.findById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Finance");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedResult() {
        when(departmentRepository.findAll(null, null, null, 0, 10, null))
                .thenReturn(List.of(stubDept(1L, 1L, "A"), stubDept(2L, 1L, "B")));
        when(departmentRepository.count(null, null, null)).thenReturn(2L);

        var result = service.findAll(null, null, null, 0, 10, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void update_happyPath_returnsUpdatedResult() {
        var dept = stubDept(1L, 2L, "Old");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(departmentRepository.existsByAreaIdAndNameAndIdNot(2L, "New", 1L)).thenReturn(false);
        when(departmentRepository.save(any())).thenReturn(stubDept(1L, 2L, "New"));

        var result = service.update(1L, new UpdateDepartmentCommand("New", "d"));

        assertThat(result.name()).isEqualTo("New");
    }

    @Test
    void update_notFound_throwsOrgNotFoundException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateDepartmentCommand("X", "d")))
                .isInstanceOf(OrgNotFoundException.class);
    }

    @Test
    void delete_happyPath_softDeletesAndSaves() {
        var dept = stubDept(1L, 2L, "HR");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        service.delete(1L);

        verify(departmentRepository).save(dept);
    }

    @Test
    void delete_notFound_throwsOrgNotFoundException() {
        when(departmentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(OrgNotFoundException.class);
    }
}
