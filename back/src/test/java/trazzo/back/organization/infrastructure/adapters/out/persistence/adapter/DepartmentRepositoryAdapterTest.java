package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import trazzo.back.organization.domain.model.business.Department;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.DepartmentEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.DepartmentJpaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepartmentRepositoryAdapterTest {

    @Mock DepartmentJpaRepository departmentRepo;
    @InjectMocks DepartmentRepositoryAdapter adapter;

    private DepartmentEntity entity() {
        var e = new DepartmentEntity();
        e.setId(1L);
        e.setAreaId(1L);
        e.setName("Matemáticas");
        e.setDescription(null);
        e.setState(true);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        Department domain = Department.restore(1L, 1L, "Matemáticas", null, true,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(departmentRepo.save(any())).thenReturn(entity());

        Department result = adapter.save(domain);

        assertThat(result.getName()).isEqualTo("Matemáticas");
    }

    @Test
    void findById_found_returnsMappedDomain() {
        when(departmentRepo.findById(1L)).thenReturn(Optional.of(entity()));

        assertThat(adapter.findById(1L)).isPresent();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(departmentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void findAll_delegatesToFindByFiltersAndMaps() {
        Page<DepartmentEntity> page = new PageImpl<>(List.of(entity()));
        when(departmentRepo.findByFilters(eq(1L), eq(true), eq("%mate%"), any(Pageable.class))).thenReturn(page);

        List<Department> result = adapter.findAll(1L, true, "mate", 0, 20, "name,asc");

        assertThat(result).hasSize(1);
    }

    @Test
    void count_delegatesToFindByFiltersTotalElements() {
        Page<DepartmentEntity> page = new PageImpl<>(List.of(entity()));
        when(departmentRepo.findByFilters(eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        assertThat(adapter.count(null, null, null)).isEqualTo(1);
    }

    @Test
    void existsByAreaIdAndName_delegates() {
        when(departmentRepo.existsByAreaIdAndName(1L, "Matemáticas")).thenReturn(true);

        assertThat(adapter.existsByAreaIdAndName(1L, "Matemáticas")).isTrue();
    }

    @Test
    void existsByAreaIdAndNameAndIdNot_delegates() {
        when(departmentRepo.existsByAreaIdAndNameAndIdNot(1L, "Matemáticas", 1L)).thenReturn(false);

        assertThat(adapter.existsByAreaIdAndNameAndIdNot(1L, "Matemáticas", 1L)).isFalse();
        verify(departmentRepo).existsByAreaIdAndNameAndIdNot(1L, "Matemáticas", 1L);
    }
}
