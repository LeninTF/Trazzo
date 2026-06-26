package trazzo.back.saasglobal.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SaasDomainExceptionsTest {

    @Test
    void invalidSubscriptionTransition_storesMessage() {
        var ex = new InvalidSubscriptionTransitionException("bad transition");
        assertThat(ex.getMessage()).isEqualTo("bad transition");
    }

    @Test
    void tenantAlreadyActivated_storesMessage() {
        var ex = new TenantAlreadyActivatedException("already active");
        assertThat(ex.getMessage()).isEqualTo("already active");
    }

    @Test
    void tenantValidation_storesMessage() {
        var ex = new TenantValidationException("invalid tenant");
        assertThat(ex.getMessage()).isEqualTo("invalid tenant");
    }

    @Test
    void userValidation_storesMessage() {
        var ex = new UserValidationException("invalid user");
        assertThat(ex.getMessage()).isEqualTo("invalid user");
    }
}
