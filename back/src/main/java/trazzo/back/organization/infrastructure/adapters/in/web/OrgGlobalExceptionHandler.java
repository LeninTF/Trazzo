package trazzo.back.organization.infrastructure.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.exception.OrgValidationException;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.ErrorResponse;

@RestControllerAdvice(assignableTypes = {
        BranchController.class,
        AreaController.class,
        DepartmentController.class,
        RoleController.class,
        PermissionController.class,
        UserRoleController.class
})
public class OrgGlobalExceptionHandler {

    @ExceptionHandler(OrgNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrgNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateOrgNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateOrgNameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(OrgValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(OrgValidationException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorResponse.ValidationDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation Error",
                        "Validation failed for one or more fields", details));
    }
}
