package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import trazzo.back.saasglobal.domain.exception.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaasGlobalExceptionHandlerTest {

    private final SaasGlobalExceptionHandler handler = new SaasGlobalExceptionHandler();

    @Test
    void badCredentials_returns401() {
        var response = handler.handleBadCredentials(new BadCredentialsException("Invalid password"));
        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Credenciales inválidas", response.getBody().message());
    }

    @Test
    void illegalArgument_returns400() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("Bad argument"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad argument", response.getBody().message());
    }

    @Test
    void illegalState_returns400() {
        var response = handler.handleIllegalState(new IllegalStateException("Bad state"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad state", response.getBody().message());
    }

    @Test
    void tenantValidation_returns400() {
        var response = handler.handleTenantValidation(new TenantValidationException("Tenant validation failed"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Validation Error", response.getBody().error());
    }

    @Test
    void userValidation_returns400() {
        var response = handler.handleUserValidation(new UserValidationException("User validation failed"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("Validation Error", response.getBody().error());
    }

    @Test
    void tenantAlreadyActivated_returns409() {
        var response = handler.handleTenantAlreadyActivated(new TenantAlreadyActivatedException("Already active"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void invalidSubscriptionTransition_returns409() {
        var response = handler.handleInvalidSubscriptionTransition(
                new InvalidSubscriptionTransitionException("Invalid subscription"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void invalidTenantTransition_returns409() {
        var response = handler.handleInvalidTenantTransition(
                new InvalidTenantTransitionException("Invalid transition"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void requestRateLimit_returns429() {
        var response = handler.handleRequestRateLimit(new RequestRateLimitException("Rate limited"));
        assertEquals(429, response.getStatusCode().value());
        assertEquals("Too Many Requests", response.getBody().error());
    }

    @Test
    void roleInUse_returns409() {
        var response = handler.handleRoleInUse(new RoleInUseException("Role is in use"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void authorizationDenied_returns403() {
        var response = handler.handleAuthorizationDenied(
                new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false)));
        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", response.getBody().error());
    }

    @Test
    void dataIntegrityViolation_returns409() {
        var response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("Duplicate key"));
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().error());
    }

    @Test
    void methodArgumentNotValid_returns400WithDetails() {
        var ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("obj", "nombre", "is required");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var response = handler.handleMethodArgumentNotValid(ex);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody().details());
        assertEquals(1, response.getBody().details().size());
        assertEquals("nombre", response.getBody().details().getFirst().field());
    }

    @Test
    void genericException_returns500() {
        var response = handler.handleGeneric(new RuntimeException("Unexpected error"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal Server Error", response.getBody().error());
    }
}
