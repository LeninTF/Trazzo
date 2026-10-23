package trazzo.back.corehr.domain.exception;

/** El payload biométrico no se pudo descifrar (clave, parámetros o integridad incorrectos). */
public class BiometricDecryptionException extends IllegalArgumentException {
    public BiometricDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
