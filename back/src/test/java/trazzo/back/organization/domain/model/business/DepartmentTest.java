package trazzo.back.organization.domain.model.business;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class DepartmentTest {

    @Test
    void create_setsDefaultStateAndTimestamps() {
        var dept = Department.create(2L, "Engineering", "Eng dept");
        assertThat(dept.getId()).isNull();
        assertThat(dept.getAreaId()).isEqualTo(2L);
        assertThat(dept.getName()).isEqualTo("Engineering");
        assertThat(dept.getDescription()).isEqualTo("Eng dept");
        assertThat(dept.isState()).isTrue();
        assertThat(dept.getCreatedAt()).isNotNull();
        assertThat(dept.getDeletedAt()).isNull();
    }

    @Test
    void create_nullAreaId_throwsValidationException() {
        assertThatThrownBy(() -> Department.create(null, "Eng", "desc"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("areaId");
    }

    @Test
    void create_nullName_throwsValidationException() {
        assertThatThrownBy(() -> Department.create(1L, null, "desc"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var dept = Department.restore(10L, 3L, "HR", "Human Resources", false, now, now, now);
        assertThat(dept.getId()).isEqualTo(10L);
        assertThat(dept.getAreaId()).isEqualTo(3L);
        assertThat(dept.getName()).isEqualTo("HR");
        assertThat(dept.isState()).isFalse();
        assertThat(dept.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void update_changesNameAndDescription() {
        var dept = Department.create(1L, "Old", null);
        dept.update("New", "new desc");
        assertThat(dept.getName()).isEqualTo("New");
        assertThat(dept.getDescription()).isEqualTo("new desc");
    }

    @Test
    void softDelete_setsStateAndDeletedAt() {
        var dept = Department.create(1L, "Eng", null);
        dept.softDelete();
        assertThat(dept.isState()).isFalse();
        assertThat(dept.getDeletedAt()).isNotNull();
    }
}
