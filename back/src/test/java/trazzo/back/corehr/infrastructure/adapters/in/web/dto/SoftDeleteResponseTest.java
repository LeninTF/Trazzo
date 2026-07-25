package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SoftDeleteResponseTest {

    @Test
    void constructor_shouldSetAllFields() {
        var now = LocalDateTime.now();
        var response = new SoftDeleteResponse(1L, "DELETED", now);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("DELETED");
        assertThat(response.deletedAt()).isEqualTo(now);
    }
}
