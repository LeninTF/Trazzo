package trazzo.back.saasglobal.domain.model.iam;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserRolesMasterTest {

    @Test
    void assign_setsUserIdAndRoleId() {
        var assignment = UserRolesMaster.assign("user-1", 5);

        assertThat(assignment.getUserId()).isEqualTo("user-1");
        assertThat(assignment.getRolesMasterId()).isEqualTo(5);
        assertThat(assignment.getCreatedAt()).isNotNull();
    }

    @Test
    void restore_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        var assignment = UserRolesMaster.restore("user-2", 3, now);

        assertThat(assignment.getUserId()).isEqualTo("user-2");
        assertThat(assignment.getRolesMasterId()).isEqualTo(3);
        assertThat(assignment.getCreatedAt()).isEqualTo(now);
    }
}
