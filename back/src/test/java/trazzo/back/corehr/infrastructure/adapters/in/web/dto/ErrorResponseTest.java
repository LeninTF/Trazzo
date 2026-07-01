package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void compactConstructorSetsTimestampAndNullDetails() {
        var now = LocalDateTime.now();
        var response = new ErrorResponse(404, "Not Found", "Resource not found");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Resource not found");
        assertThat(response.details()).isNull();
        assertThat(response.timestamp()).isAfterOrEqualTo(now);
    }

    @Test
    void compactConstructorWithDetailsSetsTimestampAndDetails() {
        var now = LocalDateTime.now();
        var details = List.of(new ErrorResponse.ValidationDetail("name", "is required"));
        var response = new ErrorResponse(400, "Bad Request", "Validation failed", details);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.details()).hasSize(1);
        assertThat(response.details().get(0).field()).isEqualTo("name");
        assertThat(response.details().get(0).message()).isEqualTo("is required");
        assertThat(response.timestamp()).isAfterOrEqualTo(now);
    }

    @Test
    void canonicalConstructor() {
        var ts = LocalDateTime.of(2025, 1, 1, 0, 0);
        var details = List.of(new ErrorResponse.ValidationDetail("field", "msg"));
        var response = new ErrorResponse(ts, 500, "Error", "msg", details);

        assertThat(response.timestamp()).isEqualTo(ts);
        assertThat(response.status()).isEqualTo(500);
        assertThat(response.details()).hasSize(1);
    }

    @Test
    void validationDetailRecord() {
        var detail = new ErrorResponse.ValidationDetail("email", "invalid format");

        assertThat(detail.field()).isEqualTo("email");
        assertThat(detail.message()).isEqualTo("invalid format");
    }

    @Test
    void equalsAndHashCode() {
        var ts = LocalDateTime.of(2025, 1, 1, 0, 0);
        var a = new ErrorResponse(ts, 400, "err", "msg", List.of());
        var b = new ErrorResponse(ts, 400, "err", "msg", List.of());

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void differentStatusNotEqual() {
        var ts = LocalDateTime.of(2025, 1, 1, 0, 0);
        var a = new ErrorResponse(ts, 400, "err", "msg", null);
        var b = new ErrorResponse(ts, 500, "err", "msg", null);

        assertThat(a).isNotEqualTo(b);
    }
}
