package trazzo.back.organization.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.exception.OrgValidationException;

import static org.assertj.core.api.Assertions.assertThat;

class OrgGlobalExceptionHandlerTest {

    private final OrgGlobalExceptionHandler handler = new OrgGlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithBody() {
        var response = handler.handleNotFound(new OrgNotFoundException("Branch not found"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Branch not found");
    }

    @Test
    void handleDuplicate_returns409WithBody() {
        var response = handler.handleDuplicate(new DuplicateOrgNameException("Name already exists"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
    }

    @Test
    void handleValidation_returns400WithBody() {
        var response = handler.handleValidation(new OrgValidationException("name is required"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Validation Error");
        assertThat(response.getBody().message()).isEqualTo("name is required");
    }

    @Test
    void handleIllegalArgument_returns400WithBody() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("bad value"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("bad value");
    }

    @Test
    void handleNotFound_nullMessage_doesNotThrow() {
        var response = handler.handleNotFound(new OrgNotFoundException(null));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
