package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantUserDepartmentEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.TenantUserDepartmentJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantUserDepartmentRepositoryAdapterTest {

    @Mock
    private TenantUserDepartmentJpaRepository tenantUserDepartmentRepo;

    @InjectMocks
    private TenantUserDepartmentRepositoryAdapter adapter;

    private final LocalDate startDate = LocalDate.of(2025, 1, 1);
    private final LocalDateTime now = LocalDateTime.now();

    private TenantUserDepartmentEntity createEntity(Long id) {
        var e = new TenantUserDepartmentEntity();
        e.setId(id);
        e.setTenantUserId(100L);
        e.setDepartmentId(200L);
        e.setPrimary(true);
        e.setStartDate(startDate);
        e.setEndDate(null);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = TenantUserDepartment.restore(null, 100L, 200L, true, startDate, null, now, now);
        var entity = createEntity(1L);
        when(tenantUserDepartmentRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantUserId()).isEqualTo(100L);
        assertThat(result.isPrimary()).isTrue();
        verify(tenantUserDepartmentRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(tenantUserDepartmentRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(tenantUserDepartmentRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByTenantUserId_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(tenantUserDepartmentRepo.findByTenantUserId(100L)).thenReturn(List.of(entity));

        var result = adapter.findAllByTenantUserId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantUserId()).isEqualTo(100L);
    }

    @Test
    void findAllByTenantUserId_whenEmpty_shouldReturnEmptyList() {
        when(tenantUserDepartmentRepo.findByTenantUserId(99L)).thenReturn(List.of());

        var result = adapter.findAllByTenantUserId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByTenantUserIdAndDepartmentId_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(tenantUserDepartmentRepo.findByTenantUserIdAndDepartmentId(100L, 200L))
                .thenReturn(Optional.of(entity));

        var result = adapter.findByTenantUserIdAndDepartmentId(100L, 200L);

        assertThat(result).isPresent();
        assertThat(result.get().getDepartmentId()).isEqualTo(200L);
    }

    @Test
    void findByTenantUserIdAndDepartmentId_whenNotExists_shouldReturnEmpty() {
        when(tenantUserDepartmentRepo.findByTenantUserIdAndDepartmentId(99L, 99L))
                .thenReturn(Optional.empty());

        var result = adapter.findByTenantUserIdAndDepartmentId(99L, 99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findPrimaryByTenantUserId_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(tenantUserDepartmentRepo.findByTenantUserIdAndIsPrimaryTrue(100L))
                .thenReturn(Optional.of(entity));

        var result = adapter.findPrimaryByTenantUserId(100L);

        assertThat(result).isPresent();
        assertThat(result.get().isPrimary()).isTrue();
    }

    @Test
    void findPrimaryByTenantUserId_whenNotExists_shouldReturnEmpty() {
        when(tenantUserDepartmentRepo.findByTenantUserIdAndIsPrimaryTrue(99L))
                .thenReturn(Optional.empty());

        var result = adapter.findPrimaryByTenantUserId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_shouldDelegate() {
        adapter.deleteById(1L);

        verify(tenantUserDepartmentRepo).deleteById(1L);
    }
}
