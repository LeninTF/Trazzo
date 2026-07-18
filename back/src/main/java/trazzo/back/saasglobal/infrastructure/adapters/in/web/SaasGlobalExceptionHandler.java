package trazzo.back.saasglobal.infrastructure.adapters.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.saasglobal.domain.exception.InvalidSubscriptionTransitionException;
import trazzo.back.saasglobal.domain.exception.InvalidTenantTransitionException;
import trazzo.back.saasglobal.domain.exception.RequestRateLimitException;
import trazzo.back.saasglobal.domain.exception.RoleInUseException;
import trazzo.back.saasglobal.domain.exception.TenantAlreadyActivatedException;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;
import trazzo.back.saasglobal.domain.exception.UserValidationException;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.ErrorResponse;
import trazzo.back.saasglobal.infrastructure.adapters.in.web.dto.ErrorResponse.ValidationDetail;

// AuthController is deliberately excluded: BadCredentialsException/AuthenticationException
// from its manual authenticationManager.authenticate() call must keep surfacing as 401 via
// Spring Security's own resolution, not get swallowed by this advice's Exception catch-all.
@RestControllerAdvice(assignableTypes = {
        PlanController.class,
        TenantController.class,
        HoldingController.class,
        FeatureController.class,
        RequestController.class,
        SaasRequestController.class,
        SaasRoleController.class,
        SaasUserController.class,
        SaasInvoiceController.class,
        TenantBillingController.class,
        SubscriptionController.class,
        PublicPlanController.class,
        SaasTenantController.class,
        ShopCheckoutController.class
})
public class SaasGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SaasGlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(TenantValidationException.class)
    public ResponseEntity<ErrorResponse> handleTenantValidation(TenantValidationException ex) {
        log.warn("Tenant validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage()));
    }

    @ExceptionHandler(UserValidationException.class)
    public ResponseEntity<ErrorResponse> handleUserValidation(UserValidationException ex) {
        log.warn("User validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage()));
    }

    @ExceptionHandler(TenantAlreadyActivatedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAlreadyActivated(TenantAlreadyActivatedException ex) {
        log.warn("Tenant already activated: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(InvalidSubscriptionTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSubscriptionTransition(InvalidSubscriptionTransitionException ex) {
        log.warn("Invalid subscription transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTenantTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTenantTransition(InvalidTenantTransitionException ex) {
        log.warn("Invalid tenant transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(RequestRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRequestRateLimit(RequestRateLimitException ex) {
        log.warn("Request rate limit: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", ex.getMessage()));
    }

    @ExceptionHandler(RoleInUseException.class)
    public ResponseEntity<ErrorResponse> handleRoleInUse(RoleInUseException ex) {
        log.warn("Role in use: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    // @PreAuthorize failures throw here (method security is AOP-based, invoked inside the
    // controller call) rather than propagating out to Spring Security's own
    // ExceptionTranslationFilter, so without this handler the generic Exception.class catch-all
    // below would swallow it into a misleading 500 instead of the correct 403.
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", "No tienes permiso para esta acción"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", "A resource with the same key already exists"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error",
                        "Validation failed for one or more fields", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "Unexpected error occurred"));
    }
}
