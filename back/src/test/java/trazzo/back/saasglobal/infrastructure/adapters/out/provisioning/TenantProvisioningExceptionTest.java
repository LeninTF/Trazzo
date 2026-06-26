package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TenantProvisioningExceptionTest {

    @Test
    void storesMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        var ex = new TenantProvisioningException("provisioning failed", cause);

        assertThat(ex.getMessage()).isEqualTo("provisioning failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
