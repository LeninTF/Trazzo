namespace Trazzo.Biometric.Agent.Backend;

internal static class BackendEndpointResolver
{
    private const string BaseUrlKey = "Backend:BaseUrl";

    public static string? ResolveSecurityPublicKeyUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: "Security:BackendPublicKeyUrl",
            endpointPathKey: "Backend:Endpoints:SecurityPublicKey",
            defaultPath: "/security/public-key");
    }

    public static string? ResolveAttendanceSyncUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: "Queue:BackendUrl",
            endpointPathKey: "Backend:Endpoints:AttendanceSync",
            defaultPath: "/asistencia/sync");
    }

    public static string? ResolveAttendanceMarkUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: null,
            endpointPathKey: "Backend:Endpoints:AttendanceMark",
            defaultPath: "/asistencia/marcar");
    }

    public static string? ResolveBiometricListUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: null,
            endpointPathKey: "Backend:Endpoints:BiometricList",
            defaultPath: "/corehr/biometria");
    }

    public static string? ResolveStartEnrollmentUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: null,
            endpointPathKey: "Backend:Endpoints:StartEnrollment",
            defaultPath: "/corehr/biometria/enroll/iniciar");
    }

    public static string? ResolvePendingEnrollmentUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: null,
            endpointPathKey: "Backend:Endpoints:PendingEnrollment",
            defaultPath: "/corehr/biometria/enroll/pendiente");
    }

    public static string? ResolveCompleteEnrollmentUrl(IConfiguration configuration)
    {
        return Resolve(
            configuration,
            legacyAbsoluteUrlKey: null,
            endpointPathKey: "Backend:Endpoints:CompleteEnrollment",
            defaultPath: "/corehr/biometria/enroll/completar");
    }

    internal static string? Resolve(
        IConfiguration configuration,
        string? legacyAbsoluteUrlKey,
        string endpointPathKey,
        string defaultPath)
    {
        string? legacyUrl = legacyAbsoluteUrlKey is null ? null : configuration[legacyAbsoluteUrlKey];
        if (!string.IsNullOrWhiteSpace(legacyUrl))
        {
            return legacyUrl;
        }

        string path = configuration[endpointPathKey] ?? defaultPath;
        if (Uri.TryCreate(path, UriKind.Absolute, out _))
        {
            return path;
        }

        string? baseUrl = configuration[BaseUrlKey];
        if (string.IsNullOrWhiteSpace(baseUrl))
        {
            return null;
        }

        return Combine(baseUrl, path);
    }

    private static string Combine(string baseUrl, string path)
    {
        string normalizedBase = baseUrl.TrimEnd('/');
        string normalizedPath = path.StartsWith('/') ? path : $"/{path}";
        return $"{normalizedBase}{normalizedPath}";
    }
}
