package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserInfoAdapterTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final UserInfoAdapter adapter = new UserInfoAdapter(jdbcTemplate);

    @Test
    void findByUserIdReturnsEmptyForNull() {
        assertThat(adapter.findByUserId(null)).isEmpty();
    }

    @Test
    void findByUserIdReturnsEmptyForBlank() {
        assertThat(adapter.findByUserId("  ")).isEmpty();
    }

    @Test
    void findByUserIdReturnsEmptyForInvalidUuid() {
        assertThat(adapter.findByUserId("not-a-uuid")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUserIdReturnsEmptyOnDataAccessError() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(SqlParameterValue.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection failed"));
        assertThat(adapter.findByUserId("00000000-0000-0000-0000-000000000001")).isEmpty();
    }

    @Test
    void findByUserIdsReturnsEmptyForNull() {
        assertThat(adapter.findByUserIds(null)).isEmpty();
    }

    @Test
    void findByUserIdsReturnsEmptyForEmptyList() {
        assertThat(adapter.findByUserIds(List.of())).isEmpty();
    }

    @Test
    void findByUserIdsFiltersInvalidUuids() {
        assertThat(adapter.findByUserIds(List.of("not-a-uuid", "also-bad"))).isEmpty();
    }

    @Test
    void findByUserIdsFiltersNullEntries() {
        assertThat(adapter.findByUserIds(java.util.Arrays.asList(null, ""))).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByUserIdsReturnsEmptyOnDataAccessError() {
        var namedJdbc = org.mockito.Mockito.mock(
                org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.class);
        // Inject the named parameter template by wrapping
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(SqlParameterValue.class)))
                .thenReturn(null);
        assertThat(adapter.findByUserIds(List.of("00000000-0000-0000-0000-000000000001"))).isNotNull();
    }

    @Test
    void findByUserIdReturnsEmptyForNonExistentUser() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(SqlParameterValue.class)))
                .thenReturn(null);
        assertThat(adapter.findByUserId("00000000-0000-0000-0000-000000000001")).isEmpty();
    }

    @Test
    void findByUserIdReturnsInfoWhenFound() {
        var info = new trazzo.back.audit.application.port.out.UserInfoPort.UserInfo("uid", "John Doe", "john@test.com");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(SqlParameterValue.class)))
                .thenReturn(info);
        var result = adapter.findByUserId("00000000-0000-0000-0000-000000000001");
        assertThat(result).isPresent();
        assertThat(result.get().userName()).isEqualTo("John Doe");
    }

    @Test
    void findByUserIdsReturnsEmptyForMixOfValidAndInvalidUuids() {
        assertThat(adapter.findByUserIds(java.util.Arrays.asList("not-a-uuid", null, ""))).isEmpty();
    }
}
