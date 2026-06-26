package trazzo.back.saasglobal.domain.model.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestCommentsTest {
    @Test
    void canInstantiate() {
        assertThat(new RequestComments()).isNotNull();
    }
}
