package trazzo.back.corehr.domain.exception;

/** Los bytes descifrados no son un template SourceAFIS: el agente envió otra cosa (400). */
public class BiometricTemplateFormatException extends IllegalArgumentException {
    public BiometricTemplateFormatException(String message) {
        super(message);
    }

    public BiometricTemplateFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
