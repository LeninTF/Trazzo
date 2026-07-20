package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.domain.model.master.StatusLogin;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.LogInHistoryEntity;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.LogInHistoryJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogInHistoryRepositoryAdapterTest {

    @Mock
    private LogInHistoryJpaRepository jpaRepository;

    @InjectMocks
    private LogInHistoryRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private LogInHistoryEntity createEntity() {
        var e = new LogInHistoryEntity();
        e.setId(UUID.randomUUID());
        e.setUserId(UUID.randomUUID());
        e.setAttemptedEmail("test@example.com");
        e.setStatus(StatusLogin.SUCCESS);
        e.setIpAddress("192.168.1.1");
        e.setUserAgent("Mozilla/5.0");
        e.setCreatedAt(now);
        return e;
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        var result = adapter.findById("00000000-0000-0000-0000-000000000001");

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
        assertThat(result.get(0).getAttemptedEmail()).isEqualTo("test@example.com");
        verify(jpaRepository).findByFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void findAll_shouldFilterByAttemptedEmail() {
        var entity = createEntity();
        var page = new PageImpl<>(List.of(entity));
        when(jpaRepository.findByFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        var result = adapter.findAll(null, "test@example.com", null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttemptedEmail()).isEqualTo("test@example.com");
    }

    @Test
    void count_shouldReturnFilteredCount() {
        when(jpaRepository.countByFilters(any(), any(), any(), any(), any()))
                .thenReturn(1L);

        var result = adapter.count(null, "test@example.com", null, null, null);

        assertThat(result).isEqualTo(1L);
    }
}
