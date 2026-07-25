package trazzo.back.organization.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.organization.infrastructure.adapters.out.persistence.repository.RolePermissionsJpaRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionsRepositoryAdapterTest {

    @Mock RolePermissionsJpaRepository repo;
    @InjectMocks RolePermissionsRepositoryAdapter adapter;

    @Test
    void findByRoleId_delegatesToRepo() {
        when(repo.findByIdRoleId(UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(List.of());
        var result = adapter.findByRoleId("00000000-0000-0000-0000-000000000001");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByRoleIdAndPermissionId_delegatesToRepo() {
        when(repo.existsByIdRoleIdAndIdPermissionId(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"))).thenReturn(true);
        var result = adapter.existsByRoleIdAndPermissionId(
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002");
        assertThat(result).isTrue();
    }

    @Test
    void deleteByRoleIdAndPermissionId_delegatesToRepo() {
        adapter.deleteByRoleIdAndPermissionId(
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002");
        verify(repo).deleteByIdRoleIdAndIdPermissionId(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"));
    }
}
