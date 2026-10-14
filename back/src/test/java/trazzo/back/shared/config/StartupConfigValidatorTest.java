package trazzo.back.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StartupConfigValidatorTest {

    private static final String SECRET_VALUE = "super-secret-value-123";

    private MockEnvironment validEnv() {
        return new MockEnvironment()
                .withProperty("app.jwt.secret", SECRET_VALUE)
                .withProperty("app.frontend-url", "https://app.example.com")
                .withProperty("mercadopago.required", "true")
                .withProperty("mercadopago.access-token", "token-value-456")
                .withProperty("mercadopago.webhook-secret", "webhook-value-789")
                .withProperty("mercadopago.use-sandbox", "false")
                .withProperty("mercadopago.back-url", "https://app.example.com");
    }

    @Test
    void validConfiguration_startsWithoutError() {
        assertThatCode(() -> new StartupConfigValidator(validEnv())).doesNotThrowAnyException();
    }

    @Test
    void emptyJwtSecret_failsNamingTheProperty() {
        MockEnvironment env = validEnv().withProperty("app.jwt.secret", "");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.secret");
    }

    @Test
    void emptyFrontendUrl_fails() {
        MockEnvironment env = validEnv().withProperty("app.frontend-url", "");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("app.frontend-url");
    }

    @Test
    void nonHttpsFrontendUrl_fails() {
        MockEnvironment env = validEnv().withProperty("app.frontend-url", "http://app.example.com");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("app.frontend-url");
    }

    @Test
    void missingAccessTokenWhileRequired_fails() {
        MockEnvironment env = validEnv().withProperty("mercadopago.access-token", "");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("mercadopago.access-token");
    }

    @Test
    void missingWebhookSecretWhileRequired_fails() {
        MockEnvironment env = validEnv().withProperty("mercadopago.webhook-secret", "");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("mercadopago.webhook-secret");
    }

    @Test
    void requiredDefaultsToTrue_whenPropertyAbsent() {
        MockEnvironment withoutRequired = new MockEnvironment()
                .withProperty("app.jwt.secret", SECRET_VALUE)
                .withProperty("app.frontend-url", "https://app.example.com")
                .withProperty("mercadopago.back-url", "https://app.example.com");
        assertThatThrownBy(() -> new StartupConfigValidator(withoutRequired))
                .hasMessageContaining("mercadopago.access-token");
    }

    @Test
    void missingMercadoPagoCredentials_areAllowedWhenNotRequired() {
        MockEnvironment env = validEnv()
                .withProperty("mercadopago.required", "false")
                .withProperty("mercadopago.access-token", "")
                .withProperty("mercadopago.webhook-secret", "");
        assertThatCode(() -> new StartupConfigValidator(env)).doesNotThrowAnyException();
    }

    @Test
    void sandboxEnabled_fails() {
        MockEnvironment env = validEnv().withProperty("mercadopago.use-sandbox", "true");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("mercadopago.use-sandbox");
    }

    @Test
    void nonHttpsBackUrl_fails() {
        MockEnvironment env = validEnv().withProperty("mercadopago.back-url", "http://localhost:4000");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("mercadopago.back-url");
    }

    @Test
    void blankBackUrl_fails() {
        MockEnvironment env = validEnv().withProperty("mercadopago.back-url", "");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("mercadopago.back-url");
    }

    @Test
    void message_neverContainsPropertyValues() {
        MockEnvironment env = validEnv()
                .withProperty("mercadopago.use-sandbox", "true")
                .withProperty("app.frontend-url", "http://leaky-host.example.com")
                .withProperty("mercadopago.access-token", "");
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .satisfies(e -> assertThat(e.getMessage())
                        .doesNotContain(SECRET_VALUE)
                        .doesNotContain("webhook-value-789")
                        .doesNotContain("leaky-host"));
    }

    @Test
    void allViolationsAreReportedTogether() {
        MockEnvironment env = new MockEnvironment();
        assertThatThrownBy(() -> new StartupConfigValidator(env))
                .hasMessageContaining("app.jwt.secret")
                .hasMessageContaining("app.frontend-url")
                .hasMessageContaining("mercadopago.access-token")
                .hasMessageContaining("mercadopago.webhook-secret")
                .hasMessageContaining("mercadopago.back-url");
    }

    @Test
    void localProfile_onlyWarns() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("mercadopago.use-sandbox", "true");
        env.setActiveProfiles("local");
        assertThatCode(() -> new StartupConfigValidator(env)).doesNotThrowAnyException();
    }

    @Test
    void violations_isEmptyForValidConfiguration() {
        assertThat(StartupConfigValidator.violations(validEnv())).isEmpty();
    }
}
