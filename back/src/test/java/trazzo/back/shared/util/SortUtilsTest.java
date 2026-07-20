package trazzo.back.shared.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SortUtilsTest {

    private static final Function<String, String> IDENTITY = Function.identity();
    private static final Function<String, String> AUDIT_MAPPER = Map.of(
            "createdAt", "created_at",
            "ipAddress", "ip_address",
            "entityId", "entity_id"
    )::get;
    private static final Function<String, String> FIELD_MAPPER = f -> switch (f) {
        case "name" -> "name";
        case "createdAt", "created_at" -> "createdAt";
        default -> "createdAt";
    };

    @Test
    void parseSort_null_returnsDefaultDescCreatedAt() {
        var sort = SortUtils.parseSort(null, IDENTITY);
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSort_blank_returnsDefaultDescCreatedAt() {
        var sort = SortUtils.parseSort("  ", IDENTITY);
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSort_fieldOnly_returnsAscending() {
        var sort = SortUtils.parseSort("name", FIELD_MAPPER);
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_fieldWithAsc_returnsAscending() {
        var sort = SortUtils.parseSort("name,asc", FIELD_MAPPER);
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_fieldWithDesc_returnsDescending() {
        var sort = SortUtils.parseSort("name,desc", FIELD_MAPPER);
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSort_unknownField_defaultsViaMapper() {
        var sort = SortUtils.parseSort("unknown,asc", FIELD_MAPPER);
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_customDefaultField() {
        var sort = SortUtils.parseSort(null, IDENTITY, "date");
        assertThat(sort.getOrderFor("date").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSort_auditFieldMapper() {
        var sort = SortUtils.parseSort("createdAt,asc", AUDIT_MAPPER);
        assertThat(sort.getOrderFor("created_at")).isNotNull();
        assertThat(sort.getOrderFor("created_at").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_throwsWhenMapperReturnsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> SortUtils.parseSort("unknown,asc", s -> null));
    }

    @Test
    void parseNativeSort_null_returnsDefault() {
        var result = SortUtils.parseNativeSort(null);
        assertThat(result.field()).isEqualTo("a.created_at");
        assertThat(result.direction()).isEqualTo("DESC");
    }

    @Test
    void parseNativeSort_blank_returnsDefault() {
        var result = SortUtils.parseNativeSort(" ");
        assertThat(result.field()).isEqualTo("a.created_at");
        assertThat(result.direction()).isEqualTo("DESC");
    }

    @Test
    void parseNativeSort_attendanceDate_asc() {
        var result = SortUtils.parseNativeSort("attendance_date,asc");
        assertThat(result.field()).isEqualTo("a.attendance_date");
        assertThat(result.direction()).isEqualTo("ASC");
    }

    @Test
    void parseNativeSort_attendanceDate_desc() {
        var result = SortUtils.parseNativeSort("attendanceDate,desc");
        assertThat(result.field()).isEqualTo("a.attendance_date");
        assertThat(result.direction()).isEqualTo("DESC");
    }

    @Test
    void parseNativeSort_checkIn_asc() {
        var result = SortUtils.parseNativeSort("checkIn,asc");
        assertThat(result.field()).isEqualTo("a.check_in");
    }

    @Test
    void parseNativeSort_checkOut_desc() {
        var result = SortUtils.parseNativeSort("check_out,desc");
        assertThat(result.field()).isEqualTo("a.check_out");
    }

    @Test
    void parseNativeSort_minutesLate() {
        var result = SortUtils.parseNativeSort("minutes_late,asc");
        assertThat(result.field()).isEqualTo("a.minutes_late");
    }

    @Test
    void parseNativeSort_state() {
        var result = SortUtils.parseNativeSort("state,desc");
        assertThat(result.field()).isEqualTo("a.state");
    }

    @Test
    void parseNativeSort_updatedAt() {
        var result = SortUtils.parseNativeSort("updatedAt,desc");
        assertThat(result.field()).isEqualTo("a.updated_at");
    }

    @Test
    void parseNativeSort_defaultField() {
        var result = SortUtils.parseNativeSort("unknown_field,asc");
        assertThat(result.field()).isEqualTo("a.created_at");
    }
}
