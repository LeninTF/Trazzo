package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.audit.domain.model.tenant.SystemAudit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SystemAuditEntity;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.SystemAuditJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemAuditRepositoryAdapterTest {

    @Mock
    private SystemAuditJpaRepository jpaRepository;

    @InjectMocks
    private SystemAuditRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private SystemAuditEntity createEntity() {
        var e = new SystemAuditEntity();
        e.setId(1L);
        e.setAccionSistema("GET");
        e.setModulo("Users");
        e.setEntidadId("user-1");
        e.setEndpoint("/api/users");
        e.setDescripcion("Fetched users");
        e.setIpAddress("192.168.1.1");
        e.setResultado("success");
        e.setDate(now);
        return e;
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity();
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnFilteredDomains() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModule()).isEqualTo("Users");
    }

    @Test
    void findAll_shouldFilterByModule() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, "Users", null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModule()).isEqualTo("Users");
    }

    @Test
    void findAll_shouldFilterBySearchTerm() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("Fetched", null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_shouldFilterByDateRange() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, null, null,
                now.minusDays(1), now.plusDays(1), PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void count_shouldReturnFilteredCount() {
        when(jpaRepository.countByFilters(any(), any(), any(), any()))
                .thenReturn(1L);

        var result = adapter.count(null, null, null, null, null);

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void count_shouldFilterByModule() {
        var entity1 = createEntity();
        var entity2 = createEntity();
        entity2.setId(2L);
        entity2.setModulo("Roles");
        when(jpaRepository.countByFilters(any(), any(), any(), any()))
                .thenReturn(1L);

        var result = adapter.count(null, "Users", null, null, null);

        assertThat(result).isEqualTo(1L);
    }
}
