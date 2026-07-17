package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.iam.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks UserJdbcRepositoryAdapter adapter;

    @Test
    @SuppressWarnings("unchecked")
    void findByEmail_returnsEmptyWhenNotFound() {
        doThrow(new EmptyResultDataAccessException(1))
                .when(jdbc).queryForObject(anyString(), any(RowMapper.class), any());

        Optional<User> result = adapter.findByEmail("notfound@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsSameUser() {
        User user = User.create(1, null, "user@test.com", null, "encoded_pass");

        User result = adapter.save(user);

        assertThat(result).isSameAs(user);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByTenantId_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<User> result = adapter.findByTenantId("tenant-1");

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllByTenantId_returnsEmptyListWhenNoRows() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        List<User> result = adapter.findAllByTenantId("tenant-1");

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllByTenantId_doesNotFilterOutSoftDeletedRows() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());
        var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        adapter.findAllByTenantId("tenant-1");

        org.mockito.Mockito.verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), any());
        assertThat(sqlCaptor.getValue()).doesNotContain("deleted_at IS NULL");
    }
}
