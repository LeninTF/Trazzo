package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.RoleEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.RoleJpaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleRepositoryAdapterTest {

    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock RoleJpaRepository roleRepo;
    @InjectMocks RoleRepositoryAdapter adapter;

    private RoleEntity entity() {
        var e = new RoleEntity();
        e.setId(ROLE_ID);
        e.setName("Supervisor");
        e.setDescription(null);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        Role domain = Role.restore(ROLE_ID.toString(), "Supervisor", null,
                LocalDateTime.now(), LocalDateTime.now());
        when(roleRepo.save(any())).thenReturn(entity());

        Role result = adapter.save(domain);

        assertThat(result.getName()).isEqualTo("Supervisor");
    }

    @Test
    void findById_found_returnsMappedDomain() {
        when(roleRepo.findById(ROLE_ID)).thenReturn(Optional.of(entity()));

        assertThat(adapter.findById(ROLE_ID.toString())).isPresent();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        UUID other = UUID.randomUUID();
        when(roleRepo.findById(other)).thenReturn(Optional.empty());

        assertThat(adapter.findById(other.toString())).isEmpty();
    }

    @Test
    void findAll_delegatesToFindByFiltersAndMaps() {
        Page<RoleEntity> page = new PageImpl<>(List.of(entity()));
        when(roleRepo.findByFilters(eq("%super%"), any(Pageable.class))).thenReturn(page);

        List<Role> result = adapter.findAll("super", 0, 20, "name,asc");

        assertThat(result).hasSize(1);
    }

    @Test
    void count_delegatesToFindByFiltersTotalElements() {
        Page<RoleEntity> page = new PageImpl<>(List.of(entity()));
        when(roleRepo.findByFilters(eq(null), any(Pageable.class))).thenReturn(page);

        assertThat(adapter.count(null)).isEqualTo(1);
    }

    @Test
    void existsByName_delegates() {
        when(roleRepo.existsByName("Supervisor")).thenReturn(true);

        assertThat(adapter.existsByName("Supervisor")).isTrue();
    }

    @Test
    void existsByNameAndIdNot_delegates() {
        when(roleRepo.existsByNameAndIdNot("Supervisor", ROLE_ID)).thenReturn(false);

        assertThat(adapter.existsByNameAndIdNot("Supervisor", ROLE_ID.toString())).isFalse();
    }

    @Test
    void deleteById_delegates() {
        adapter.deleteById(ROLE_ID.toString());

        verify(roleRepo).deleteById(ROLE_ID);
    }
}
