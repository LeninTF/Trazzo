package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRolesMasterJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks UserRolesMasterJdbcRepositoryAdapter adapter;

    @Test
    void findRoleIdsForUser_delegatesToJdbc() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), anyString()))
                .thenReturn(List.of(1, 2, 3));

        var result = adapter.findRoleIdsForUser("00000000-0000-0000-0000-000000000001");

        assertThat(result).containsExactly(1, 2, 3);
        verify(jdbc).queryForList(contains("roles_master_id"), eq(Integer.class), eq("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void findRoleIdsForUser_returnsEmptyList() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), anyString()))
                .thenReturn(List.of());

        var result = adapter.findRoleIdsForUser("00000000-0000-0000-0000-000000000001");

        assertThat(result).isEmpty();
    }

    @Test
    void replaceForUser_deletesThenInserts() {
        adapter.replaceForUser("00000000-0000-0000-0000-000000000001", List.of(1, 2));

        verify(jdbc).update(contains("DELETE FROM user_roles_master"), eq("00000000-0000-0000-0000-000000000001"));
        verify(jdbc, times(2)).update(contains("INSERT INTO user_roles_master"), anyString(), anyInt());
    }

    @Test
    void replaceForUser_withEmptyList_onlyDeletes() {
        adapter.replaceForUser("00000000-0000-0000-0000-000000000001", List.of());

        verify(jdbc).update(contains("DELETE"), eq("00000000-0000-0000-0000-000000000001"));
        verify(jdbc, never()).update(contains("INSERT"), anyString(), anyInt());
    }
}
