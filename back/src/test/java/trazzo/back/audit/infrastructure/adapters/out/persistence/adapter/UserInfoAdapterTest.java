package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UserInfoAdapterTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final UserInfoAdapter adapter = new UserInfoAdapter(jdbcTemplate);

    @Test
    void findByUserIdReturnsEmptyForNull() {
        var result = adapter.findByUserId(null);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdReturnsEmptyForBlank() {
        var result = adapter.findByUserId("  ");
        assertThat(result).isEmpty();
    }
}
