package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceListResponseTest {

    @Test
    void fromMapsPaginatedResultWithScope() {
        var now = LocalDateTime.now();
        var result = new AttendanceResult("id-1", 10L, null, null, null, null, null,
                now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        var paginated = new PaginatedResult<AttendanceResult>(List.of(result), 1, 10, 50, 5);

        var response = AttendanceListResponse.from(paginated, "MONTH");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo("id-1");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(50);
        assertThat(response.totalPages()).isEqualTo(5);
        assertThat(response.scopeAplicado()).isEqualTo("MONTH");
    }

    @Test
    void fromWithEmptyContent() {
        var paginated = new PaginatedResult<AttendanceResult>(List.of(), 0, 10, 0, 0);
        var response = AttendanceListResponse.from(paginated, "YEAR");

        assertThat(response.content()).isEmpty();
        assertThat(response.scopeAplicado()).isEqualTo("YEAR");
    }

    @Test
    void equalsAndHashCode() {
        var a = new AttendanceListResponse(List.of(), 0, 0, 0, 0, "SCOPE");
        var b = new AttendanceListResponse(List.of(), 0, 0, 0, 0, "SCOPE");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
