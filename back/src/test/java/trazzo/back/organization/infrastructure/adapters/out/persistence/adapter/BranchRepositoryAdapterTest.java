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
import trazzo.back.organization.domain.model.business.Branch;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.BranchEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.BranchJpaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BranchRepositoryAdapterTest {

    @Mock BranchJpaRepository branchRepo;
    @InjectMocks BranchRepositoryAdapter adapter;

    private BranchEntity entity() {
        var e = new BranchEntity();
        e.setId(1L);
        e.setName("Sede Norte");
        e.setDescription("Principal");
        e.setState(true);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        Branch domain = Branch.restore(1L, "Sede Norte", "Principal", true,
                LocalDateTime.now(), LocalDateTime.now(), null);
        when(branchRepo.save(any())).thenReturn(entity());

        Branch result = adapter.save(domain);

        assertThat(result.getName()).isEqualTo("Sede Norte");
    }

    @Test
    void findById_found_returnsMappedDomain() {
        when(branchRepo.findById(1L)).thenReturn(Optional.of(entity()));

        Optional<Branch> result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Sede Norte");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(branchRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void findAll_delegatesToFindByFiltersAndMaps() {
        Page<BranchEntity> page = new PageImpl<>(List.of(entity()));
        when(branchRepo.findByFilters(eq(true), eq("%sede%"), any(Pageable.class))).thenReturn(page);

        List<Branch> result = adapter.findAll(true, "sede", 0, 20, "name,asc");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Sede Norte");
    }

    @Test
    void count_delegatesToFindByFiltersTotalElements() {
        Page<BranchEntity> page = new PageImpl<>(List.of(entity()));
        when(branchRepo.findByFilters(eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        long count = adapter.count(null, null);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void existsByName_delegates() {
        when(branchRepo.existsByName("Sede Norte")).thenReturn(true);

        assertThat(adapter.existsByName("Sede Norte")).isTrue();
    }

    @Test
    void existsByNameAndIdNot_delegates() {
        when(branchRepo.existsByNameAndIdNot("Sede Norte", 1L)).thenReturn(false);

        assertThat(adapter.existsByNameAndIdNot("Sede Norte", 1L)).isFalse();
        verify(branchRepo).existsByNameAndIdNot("Sede Norte", 1L);
    }
}
