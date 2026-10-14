package trazzo.back.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EncryptionServiceConditionTest {

    private static final String KEY = "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EncryptionService.class);

    @Test
    void beanIsCreatedWhenKeyIsConfigured() {
        runner.withPropertyValues("app.encryption.key=" + KEY)
                .run(ctx -> assertThat(ctx).hasSingleBean(EncryptionService.class));
    }

    @Test
    void beanIsSkippedWhenKeyIsEmpty() {
        runner.withPropertyValues("app.encryption.key=")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(EncryptionService.class));
    }

    @Test
    void beanIsSkippedWhenKeyIsAbsent() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(EncryptionService.class));
    }
}
