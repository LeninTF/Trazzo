package trazzo.back.organization.domain;

import trazzo.back.organization.domain.exception.OrgValidationException;

public final class OrgValidation {

    private OrgValidation() {}

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    public static Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value;
    }
}
