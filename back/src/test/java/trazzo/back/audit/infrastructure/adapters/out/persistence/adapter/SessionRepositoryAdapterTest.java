package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SessionEntity;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.SessionJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRepositoryAdapterTest {

    @Mock
    private SessionJpaRepository jpaRepository;

    @InjectMocks
    private SessionRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private SessionEntity createEntity() {
        var e = new SessionEntity();
        e.setId(1L);
        e.setTenantUserId("user-1");
        e.setRefreshTokenHash("hash123");
        e.setIpAddress("192.168.1.1");
        e.setUserAgent("Mozilla/5.0");
        e.setDeviceFingerprint("fp-001");
        e.setLoginAt(now);
        e.setLastActivityAt(now.plusHours(1));
        e.setExpiresAt(now.plusDays(7));
        e.setState(SessionState.ACTIVE);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity();
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getTenantUserId()).isEqualTo("user-1");
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
        when(jpaRepository.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantUserId()).isEqualTo("user-1");
    }

    @Test
    void findAll_shouldFilterByTenantUserId() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("user-1", null, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantUserId()).isEqualTo("user-1");
    }

    @Test
    void findAll_shouldFilterByState() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, SessionState.ACTIVE, null, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_shouldFilterByIpAddress() {
        var entity = createEntity();
        when(jpaRepository.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, null, "10.0.0.1", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void count_shouldReturnFilteredCount() {
        when(jpaRepository.countByFilters(any(), any(), any()))
                .thenReturn(1L);

        var result = adapter.count(null, null, null);

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void count_shouldFilterByState() {
        var entity1 = createEntity();
        var entity2 = createEntity();
        entity2.setId(2L);
        entity2.setLogoutAt(now);
        entity2.setState(SessionState.LOGGED_OUT);
        when(jpaRepository.countByFilters(any(), any(), any()))
                .thenReturn(1L);

        var result = adapter.count(null, SessionState.ACTIVE, null);

        assertThat(result).isEqualTo(1L);
    }
}
