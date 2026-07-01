package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.AttendanceState;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class PatchAttendanceRequestTest {
    @Test
    void constructorAndGetters() {
        var ci = LocalDateTime.now();
        var co = ci.plusHours(8);
        var r = new PatchAttendanceRequest(ci, co, AttendanceState.TARDANZA, 15);
        assertThat(r.checkIn()).isEqualTo(ci);
        assertThat(r.checkOut()).isEqualTo(co);
        assertThat(r.state()).isEqualTo(AttendanceState.TARDANZA);
        assertThat(r.minutesLate()).isEqualTo(15);
    }
    @Test
    void allowsAllNull() {
        var r = new PatchAttendanceRequest(null, null, null, null);
        assertThat(r.checkIn()).isNull();
        assertThat(r.state()).isNull();
        assertThat(r.minutesLate()).isNull();
    }
    @Test
    void equalsAndHashCode() {
        var ci = LocalDateTime.now();
        assertThat(new PatchAttendanceRequest(ci, null, AttendanceState.PUNTUAL, 0))
                .isEqualTo(new PatchAttendanceRequest(ci, null, AttendanceState.PUNTUAL, 0))
                .hasSameHashCodeAs(new PatchAttendanceRequest(ci, null, AttendanceState.PUNTUAL, 0));
    }
}
