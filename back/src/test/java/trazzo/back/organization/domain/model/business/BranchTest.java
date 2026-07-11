package trazzo.back.organization.domain.model.business;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class BranchTest {

    @Test
    void create_setsDefaultStateAndTimestamps() {
        var branch = Branch.create("Main", "Main office");
        assertThat(branch.getId()).isNull();
        assertThat(branch.getName()).isEqualTo("Main");
        assertThat(branch.getDescription()).isEqualTo("Main office");
        assertThat(branch.isState()).isTrue();
        assertThat(branch.getCreatedAt()).isNotNull();
        assertThat(branch.getUpdatedAt()).isNotNull();
        assertThat(branch.getDeletedAt()).isNull();
    }

    @Test
    void create_trimsName() {
        var branch = Branch.create("  HQ  ", null);
        assertThat(branch.getName()).isEqualTo("HQ");
    }

    @Test
    void create_nullName_throwsValidationException() {
        assertThatThrownBy(() -> Branch.create(null, "desc"))
                .isInstanceOf(OrgValidationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_blankName_throwsValidationException() {
        assertThatThrownBy(() -> Branch.create("   ", "desc"))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void restore_setsAllFields() {
        var created = LocalDateTime.now().minusDays(1);
        var updated = LocalDateTime.now();
        var deleted = LocalDateTime.now().plusDays(1);
        var branch = Branch.restore(1L, "HQ", "Headquarters", false, created, updated, deleted);
        assertThat(branch.getId()).isEqualTo(1L);
        assertThat(branch.getName()).isEqualTo("HQ");
        assertThat(branch.getDescription()).isEqualTo("Headquarters");
        assertThat(branch.isState()).isFalse();
        assertThat(branch.getCreatedAt()).isEqualTo(created);
        assertThat(branch.getUpdatedAt()).isEqualTo(updated);
        assertThat(branch.getDeletedAt()).isEqualTo(deleted);
    }

    @Test
    void update_changesNameAndDescription() {
        var branch = Branch.create("Old", "old desc");
        branch.update("New", "new desc");
        assertThat(branch.getName()).isEqualTo("New");
        assertThat(branch.getDescription()).isEqualTo("new desc");
        assertThat(branch.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_nullName_throwsValidationException() {
        var branch = Branch.create("Main", null);
        assertThatThrownBy(() -> branch.update(null, "desc"))
                .isInstanceOf(OrgValidationException.class);
    }

    @Test
    void softDelete_setsStateAndDeletedAt() {
        var branch = Branch.create("Main", null);
        branch.softDelete();
        assertThat(branch.isState()).isFalse();
        assertThat(branch.getDeletedAt()).isNotNull();
    }
}
