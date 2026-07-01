package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateShiftRequestTest {

    @Test
    void constructorAndGetters() {
        var request = new CreateShiftRequest("Morning", "Morning shift description");

        assertThat(request.name()).isEqualTo("Morning");
        assertThat(request.description()).isEqualTo("Morning shift description");
    }

    @Test
    void allowsNullFields() {
        var request = new CreateShiftRequest(null, null);

        assertThat(request.name()).isNull();
        assertThat(request.description()).isNull();
    }

    @Test
    void equalsAndHashCode() {
        var a = new CreateShiftRequest("name", "desc");
        var b = new CreateShiftRequest("name", "desc");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
