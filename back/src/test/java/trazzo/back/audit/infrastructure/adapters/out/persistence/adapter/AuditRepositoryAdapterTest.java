package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.AuditEntity;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.AuditJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRepositoryAdapterTest {

    @Mock
    private AuditJpaRepository jpaRepository;

    @InjectMocks
    private AuditRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private AuditEntity createEntity() {
        var e = new AuditEntity();
        e.setId(UUID.randomUUID());
        e.setEntity("User");
        e.setEntityId("user-1");
        e.setAction(Action.CREATE);
        e.setUserId(UUID.randomUUID());
        e.setEndpoint("/api/users");
        e.setIpAddress("192.168.1.1");
        e.setUserAgent("Mozilla/5.0");
        e.setCreatedAt(now);
        return e;
    }

    @Test
    void findById_shouldReturnEmpty() {
        var result = adapter.findById("00000000-0000-0000-0000-000000000001");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedDomains() {
        var entity = createEntity();
        var page = new PageImpl<>(List.of(entity));
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        var result = adapter.findAll("search", Action.CREATE, "User",
                now, now.plusDays(1), PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntity()).isEqualTo("User");
        verify(jpaRepository).findByFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void count_shouldReturnTotalElements() {
        when(jpaRepository.countByFilters(any(), any(), any(), any(), any()))
                .thenReturn(5L);

        var result = adapter.count("search", Action.CREATE, "User",
                now, now.plusDays(1));

        assertThat(result).isEqualTo(5L);
    }
}
