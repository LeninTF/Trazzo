package trazzo.back.organization.domain.model.business;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class AreaTest {

    @Test
    void create_setsDefaultStateAndTimestamps() {
        var area = Area.create(1L, "Sales", "Sales area");
        assertThat(area.getId()).isNull();
        assertThat(area.getBranchId()).isEqualTo(1L);
        assertThat(area.getName()).isEqualTo("Sales");
        assertThat(area.getDescription()).isEqualTo("Sales area");
        assertThat(area.isState()).isTrue();
        assertThat(area.getCreatedAt()).isNotNull();
        assertThat(area.getDeletedAt()).isNull();
    }

    @Test
    void create_nullBranchId_throwsValidationException() {
        assertThatThrownBy(() -> Area.create(null, "Sales", "desc"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("branchId");
    }

    @Test
    void create_nullName_throwsValidationException() {
        assertThatThrownBy(() -> Area.create(1L, null, "desc"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_blankName_throwsValidationException() {
        assertThatThrownBy(() -> Area.create(1L, "  ", "desc"))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var area = Area.restore(5L, 2L, "IT", "IT dept", true, now, now, null);
        assertThat(area.getId()).isEqualTo(5L);
        assertThat(area.getBranchId()).isEqualTo(2L);
        assertThat(area.getName()).isEqualTo("IT");
        assertThat(area.isState()).isTrue();
    }

    @Test
    void update_changesNameAndDescription() {
        var area = Area.create(1L, "Old", null);
        area.update("New", "new desc");
        assertThat(area.getName()).isEqualTo("New");
        assertThat(area.getDescription()).isEqualTo("new desc");
    }

    @Test
    void update_blankName_throwsValidationException() {
        var area = Area.create(1L, "Sales", null);
        assertThatThrownBy(() -> area.update("", "desc"))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void softDelete_setsStateAndDeletedAt() {
        var area = Area.create(1L, "Sales", null);
        area.softDelete();
        assertThat(area.isState()).isFalse();
        assertThat(area.getDeletedAt()).isNotNull();
    }
}
