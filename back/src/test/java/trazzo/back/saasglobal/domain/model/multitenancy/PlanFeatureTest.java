package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlanFeatureTest {
    @Test
    void canInstantiate() {
        assertThat(new PlanFeature()).isNotNull();
    }
}
