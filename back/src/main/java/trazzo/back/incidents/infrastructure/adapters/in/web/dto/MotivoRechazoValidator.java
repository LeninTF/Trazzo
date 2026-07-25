package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import trazzo.back.incidents.domain.model.IncidentState;

public class MotivoRechazoValidator implements ConstraintValidator<ValidMotivoRechazo, IncidentStateChangeRequest> {

    private static final String MOTIVO_RECHAZO_FIELD = "motivo_rechazo";

    @Override
    public boolean isValid(IncidentStateChangeRequest request, ConstraintValidatorContext context) {
        if (request == null || request.state() != IncidentState.DENEGADO) {
            return true;
        }
        var motivo = request.motivoRechazo();
        if (motivo != null && !motivo.isBlank()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(MOTIVO_RECHAZO_FIELD)
                .addConstraintViolation();
        return false;
    }
}

