package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoAdapterTest {

    private final UserInfoAdapter adapter = new UserInfoAdapter();

    @Test
    void findByUserIdReturnsEmpty() {
        var result = adapter.findByUserId("any-user");
        assertThat(result).isEmpty();
    }
}
