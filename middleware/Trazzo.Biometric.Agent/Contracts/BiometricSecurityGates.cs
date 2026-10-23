namespace Trazzo.Biometric.Agent.Contracts;

// Gate global de seguridad para el envío de plantillas biométricas.
// Por defecto se bloquea el fallback plaintext: si la clave RSA no está configurada,
// los mensajes salen SIN plantilla en vez de exponerla en claro.
// Para habilitarlo en desarrollo local: setear TRAZZO_ALLOW_PLAINTEXT_TEMPLATES=true.
public static class BiometricSecurityGates
{
    public static bool AllowPlaintextTemplateFallback { get; set; } =
        string.Equals(
            Environment.GetEnvironmentVariable("TRAZZO_ALLOW_PLAINTEXT_TEMPLATES"),
            "true",
            StringComparison.OrdinalIgnoreCase);
}
