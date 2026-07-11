package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.TenantSettingsRecordEntity;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.TenantSettingsRecordJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantSettingsRecordRepositoryAdapterTest {

    @Mock
    private TenantSettingsRecordJpaRepository jpaRepository;

    @InjectMocks
    private TenantSettingsRecordRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private TenantSettingsRecordEntity createEntity() {
        var e = new TenantSettingsRecordEntity();
        e.setId(1L);
        e.setTenantSettingId("setting-1");
        e.setDbName("mydb");
        e.setDbHost("localhost");
        e.setDbUser("admin");
        e.setDbPassword("pass123");
        e.setUserId("user-1");
        e.setChangeReason("Schema update");
        e.setCreatedAt(now);
        return e;
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity();
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getChangeReason()).isEqualTo("Schema update");
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedDomains() {
        var entity = createEntity();
        var page = new PageImpl<>(List.of(entity));
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        var result = adapter.findAll(null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDbName()).isEqualTo("mydb");
        verify(jpaRepository).findByFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void count_shouldReturnTotalElements() {
        var page = mock(org.springframework.data.domain.Page.class);
        when(page.getTotalElements()).thenReturn(3L);
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        var result = adapter.count(null, null, null, null, null);

        assertThat(result).isEqualTo(3L);
    }
}
