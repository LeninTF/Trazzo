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
import trazzo.back.organization.domain.model.business.Area;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.AreaEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.AreaJpaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AreaRepositoryAdapterTest {

    @Mock AreaJpaRepository areaRepo;
    @InjectMocks AreaRepositoryAdapter adapter;

    private AreaEntity entity() {
        var e = new AreaEntity();
        e.setId(1L);
        e.setBranchId(1L);
        e.setName("Dirección Académica");
        e.setDescription(null);
        e.setState(true);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        Area domain = Area.restore(1L, 1L, "Dirección Académica", null, true,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(areaRepo.save(any())).thenReturn(entity());

        Area result = adapter.save(domain);

        assertThat(result.getName()).isEqualTo("Dirección Académica");
    }

    @Test
    void findById_found_returnsMappedDomain() {
        when(areaRepo.findById(1L)).thenReturn(Optional.of(entity()));

        assertThat(adapter.findById(1L)).isPresent();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(areaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void findAll_delegatesToFindByFiltersAndMaps() {
        Page<AreaEntity> page = new PageImpl<>(List.of(entity()));
        when(areaRepo.findByFilters(eq(1L), eq(true), eq("%area%"), any(Pageable.class))).thenReturn(page);

        List<Area> result = adapter.findAll(1L, true, "area", 0, 20, "name,asc");

        assertThat(result).hasSize(1);
    }

    @Test
    void count_delegatesToFindByFiltersTotalElements() {
        Page<AreaEntity> page = new PageImpl<>(List.of(entity()));
        when(areaRepo.findByFilters(eq(null), eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        assertThat(adapter.count(null, null, null)).isEqualTo(1);
    }

    @Test
    void existsByBranchIdAndName_delegates() {
        when(areaRepo.existsByBranchIdAndName(1L, "Area")).thenReturn(true);

        assertThat(adapter.existsByBranchIdAndName(1L, "Area")).isTrue();
    }

    @Test
    void existsByBranchIdAndNameAndIdNot_delegates() {
        when(areaRepo.existsByBranchIdAndNameAndIdNot(1L, "Area", 1L)).thenReturn(false);

        assertThat(adapter.existsByBranchIdAndNameAndIdNot(1L, "Area", 1L)).isFalse();
        verify(areaRepo).existsByBranchIdAndNameAndIdNot(1L, "Area", 1L);
    }
}
