package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.corehr.domain.model.employee.TenantContact;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantContactEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.TenantContactJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantContactRepositoryAdapterTest {

    @Mock
    private TenantContactJpaRepository tenantContactRepo;

    @InjectMocks
    private TenantContactRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private TenantContactEntity createEntity(Long id) {
        var e = new TenantContactEntity();
        e.setId(id);
        e.setTenantUserId(100L);
        e.setType("EMAIL");
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setDeletedAt(null);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = TenantContact.restore(null, 100L, "EMAIL", now, now, null);
        var entity = createEntity(1L);
        when(tenantContactRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("EMAIL");
        verify(tenantContactRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(tenantContactRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(tenantContactRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(tenantContactRepo.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void count_shouldReturnTotalCount() {
        when(tenantContactRepo.count()).thenReturn(5L);

        var result = adapter.count();

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void findByTenantUserId_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(tenantContactRepo.findByTenantUserId(100L)).thenReturn(List.of(entity));

        var result = adapter.findByTenantUserId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantUserId()).isEqualTo(100L);
    }

    @Test
    void findByTenantUserId_whenEmpty_shouldReturnEmptyList() {
        when(tenantContactRepo.findByTenantUserId(99L)).thenReturn(List.of());

        var result = adapter.findByTenantUserId(99L);

        assertThat(result).isEmpty();
    }
}
