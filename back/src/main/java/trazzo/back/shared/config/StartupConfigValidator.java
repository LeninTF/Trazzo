package trazzo.back.shared.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails the boot outside the {@code local} profile when the configuration is unsafe for a real
 * deployment. Messages name the property, never its value, so nothing sensitive reaches the logs.
 * In {@code local} the same findings are only logged as warnings.
 */
@Component
public class StartupConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigValidator.class);
    private static final String HTTPS = "https://";

    public StartupConfigValidator(Environment env) {
        List<String> problems = violations(env);
        if (problems.isEmpty()) {
            return;
        }
        if (env.acceptsProfiles(Profiles.of("local"))) {
            problems.forEach(p -> log.warn("Configuration warning (profile local): {}", p));
            return;
        }
        throw new IllegalStateException("Invalid configuration, refusing to start: " + String.join("; ", problems));
    }

    static List<String> violations(Environment env) {
        List<String> problems = new ArrayList<>();
        if (!StringUtils.hasText(env.getProperty("app.jwt.secret"))) {
            problems.add("app.jwt.secret must not be empty");
        }
        if (!isHttps(env.getProperty("app.frontend-url"))) {
            problems.add("app.frontend-url must be set and start with https://");
        }
        if (env.getProperty("mercadopago.required", Boolean.class, true)) {
            if (!StringUtils.hasText(env.getProperty("mercadopago.access-token"))) {
                problems.add("mercadopago.access-token must not be empty while mercadopago.required is true");
            }
            if (!StringUtils.hasText(env.getProperty("mercadopago.webhook-secret"))) {
                problems.add("mercadopago.webhook-secret must not be empty while mercadopago.required is true");
            }
        }
        if (env.getProperty("mercadopago.use-sandbox", Boolean.class, false)) {
            problems.add("mercadopago.use-sandbox must be false");
        }
        if (!isHttps(env.getProperty("mercadopago.back-url"))) {
            problems.add("mercadopago.back-url must be set and start with https://");
        }
        if (!StringUtils.hasText(env.getProperty("app.biometric.rsa.private-key"))
                && !StringUtils.hasText(env.getProperty("app.biometric.rsa.key-file"))) {
            // Sin una clave RSA persistente, cada reinicio deja indescifrables los templates enrolados.
            problems.add("app.biometric.rsa.private-key or app.biometric.rsa.key-file must be set");
        }
        if (!StringUtils.hasText(env.getProperty("app.biometric.storage-key"))
                && !StringUtils.hasText(env.getProperty("app.encryption.key"))) {
            // Sin clave de almacenamiento no se puede guardar ninguna huella enrolada.
            problems.add("app.biometric.storage-key (or its fallback app.encryption.key) must be set");
        }
        return problems;
    }

    private static boolean isHttps(String url) {
        return url != null && url.regionMatches(true, 0, HTTPS, 0, HTTPS.length());
    }
}
