package trazzo.back.saasglobal.infrastructure.adapters.out.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SpringAppConfigAdapterTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SpringAppConfigAdapter.class);

    @Test
    void exposesConfiguredValues() {
        runner.withPropertyValues(
                        "app.frontend-url=https://app.example.com",
                        "mercadopago.back-url=https://back.example.com",
                        "mercadopago.use-sandbox=true",
                        "trazzo.requests.notification-email=ops@example.com")
                .run(ctx -> {
                    SpringAppConfigAdapter adapter = ctx.getBean(SpringAppConfigAdapter.class);
                    assertThat(adapter.frontendUrl()).isEqualTo("https://app.example.com");
                    assertThat(adapter.mercadoPagoBackUrl()).isEqualTo("https://back.example.com");
                    assertThat(adapter.mercadoPagoUseSandbox()).isTrue();
                    assertThat(adapter.requestsNotificationEmail()).isEqualTo("ops@example.com");
                });
    }

    @Test
    void notificationEmailKeepsItsDefault() {
        runner.withPropertyValues(
                        "app.frontend-url=https://app.example.com",
                        "mercadopago.back-url=https://app.example.com",
                        "mercadopago.use-sandbox=false")
                .run(ctx -> assertThat(ctx.getBean(SpringAppConfigAdapter.class).requestsNotificationEmail())
                        .isEqualTo("solicitudes@trazzo.pe"));
    }

    @Test
    void missingFrontendUrl_failsInsteadOfFallingBackToADevUrl() {
        // Boot registers a strict PropertySourcesPlaceholderConfigurer; the bare runner would not.
        runner.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues("mercadopago.back-url=https://x", "mercadopago.use-sandbox=false")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
