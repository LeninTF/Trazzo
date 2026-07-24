package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MotivoRechazoValidator.class)
public @interface ValidMotivoRechazo {

    String message() default "motivo_rechazo is required when state is DENEGADO";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
