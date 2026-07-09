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
import trazzo.back.organization.domain.model.roles.Permissions;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.PermissionEntity;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.PermissionJpaRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionRepositoryAdapterTest {

    private static final UUID PERMISSION_ID = UUID.randomUUID();

    @Mock PermissionJpaRepository permissionRepo;
    @InjectMocks PermissionRepositoryAdapter adapter;

    private PermissionEntity entity() {
        var e = new PermissionEntity();
        e.setId(PERMISSION_ID);
        e.setName("read:users");
        e.setDescription(null);
        e.setMasterFeaturesCode(null);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    @Test
    void save_mapsDomainToEntityAndBack() {
        Permissions domain = Permissions.restore(PERMISSION_ID.toString(), "read:users", null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(permissionRepo.save(any())).thenReturn(entity());

        Permissions result = adapter.save(domain);

        assertThat(result.getName()).isEqualTo("read:users");
    }

    @Test
    void findById_found_returnsMappedDomain() {
        when(permissionRepo.findById(PERMISSION_ID)).thenReturn(Optional.of(entity()));

        assertThat(adapter.findById(PERMISSION_ID.toString())).isPresent();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        UUID other = UUID.randomUUID();
        when(permissionRepo.findById(other)).thenReturn(Optional.empty());

        assertThat(adapter.findById(other.toString())).isEmpty();
    }

    @Test
    void findAll_delegatesToFindByFiltersAndMaps() {
        Page<PermissionEntity> page = new PageImpl<>(List.of(entity()));
        when(permissionRepo.findByFilters(eq("%read%"), any(Pageable.class))).thenReturn(page);

        List<Permissions> result = adapter.findAll("read", 0, 20, "name,asc");

        assertThat(result).hasSize(1);
    }

    @Test
    void count_delegatesToFindByFiltersTotalElements() {
        Page<PermissionEntity> page = new PageImpl<>(List.of(entity()));
        when(permissionRepo.findByFilters(eq(null), any(Pageable.class))).thenReturn(page);

        assertThat(adapter.count(null)).isEqualTo(1);
    }

    @Test
    void existsByName_delegates() {
        when(permissionRepo.existsByName("read:users")).thenReturn(true);

        assertThat(adapter.existsByName("read:users")).isTrue();
    }

    @Test
    void existsByNameAndIdNot_delegates() {
        when(permissionRepo.existsByNameAndIdNot("read:users", PERMISSION_ID)).thenReturn(false);

        assertThat(adapter.existsByNameAndIdNot("read:users", PERMISSION_ID.toString())).isFalse();
    }

    @Test
    void deleteById_delegates() {
        adapter.deleteById(PERMISSION_ID.toString());

        verify(permissionRepo).deleteById(PERMISSION_ID);
    }
}
