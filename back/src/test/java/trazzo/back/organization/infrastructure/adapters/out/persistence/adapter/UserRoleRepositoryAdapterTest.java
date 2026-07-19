package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.TenantUserRoleJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleRepositoryAdapterTest {

    @Mock TenantUserRoleJpaRepository repo;
    @InjectMocks UserRoleRepositoryAdapter adapter;

    @Test
    void findById_returnsEmpty() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThat(adapter.findById(1L)).isEmpty();
    }

    @Test
    void findByTenantUserId_delegatesToRepo() {
        when(repo.findByTenantUserId(1L)).thenReturn(List.of());
        var result = adapter.findByTenantUserId(1L);
        assertThat(result).isEmpty();
        verify(repo).findByTenantUserId(1L);
    }

    @Test
    void findByRoleId_delegatesToRepo() {
        when(repo.findByRoleId(UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(List.of());
        var result = adapter.findByRoleId("00000000-0000-0000-0000-000000000001");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByTenantUserIdAndRoleIdAndDepartmentId_delegatesToRepo() {
        when(repo.existsByTenantUserIdAndRoleIdAndDepartmentId(
                1L, UUID.fromString("00000000-0000-0000-0000-000000000001"), 10L)).thenReturn(true);
        var result = adapter.existsByTenantUserIdAndRoleIdAndDepartmentId(
                1L, "00000000-0000-0000-0000-000000000001", 10L);
        assertThat(result).isTrue();
    }

    @Test
    void deleteById_delegatesToRepo() {
        adapter.deleteById(1L);
        verify(repo).deleteById(1L);
    }
}
