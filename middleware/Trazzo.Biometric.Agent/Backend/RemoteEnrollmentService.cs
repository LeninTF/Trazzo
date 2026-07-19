using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Security;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.Backend;

public sealed class RemoteEnrollmentService : BackgroundService
{
    // 16 KB para sesión pendiente (token + metadata).
    private static readonly HttpClient SharedHttpClient = new()
    {
        Timeout = TimeSpan.FromSeconds(30),
        MaxResponseContentBufferSize = 16 * 1024
    };
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly IBiometricScannerService _scannerService;
    private readonly HttpClient _httpClient;
    private readonly ILogger<RemoteEnrollmentService> _logger;
    private readonly Func<TimeSpan, CancellationToken, Task> _delay;
    private readonly string? _pendingEnrollmentUrl;
    private readonly string? _completeEnrollmentUrl;
    private readonly string? _tenantId;
    private readonly string? _agentToken;
    private readonly string? _deviceCode;
    private readonly TimeSpan _pollingInterval;
    private readonly bool _isEnabled;

    public RemoteEnrollmentService(
        IBiometricScannerService scannerService,
        IConfiguration configuration,
        ILogger<RemoteEnrollmentService> logger)
        : this(scannerService, configuration, logger, SharedHttpClient)
    {
    }

    internal RemoteEnrollmentService(
        IBiometricScannerService scannerService,
        IConfiguration configuration,
        ILogger<RemoteEnrollmentService> logger,
        HttpClient httpClient,
        Func<TimeSpan, CancellationToken, Task>? delay = null)
    {
        _scannerService = scannerService;
        _logger = logger;
        _httpClient = httpClient;
        _delay = delay ?? Task.Delay;

        _pendingEnrollmentUrl = BackendEndpointResolver.EnsureSecureUrl(
            BackendEndpointResolver.ResolvePendingEnrollmentUrl(configuration),
            logger,
            "Backend:Endpoints:PendingEnrollment");
        _completeEnrollmentUrl = BackendEndpointResolver.EnsureSecureUrl(
            BackendEndpointResolver.ResolveCompleteEnrollmentUrl(configuration),
            logger,
            "Backend:Endpoints:CompleteEnrollment");
        _tenantId = configuration["Agent:TenantId"];
        _agentToken = AgentTokenProtector.ResolveAgentToken(configuration, logger);
        _deviceCode = configuration["Agent:DeviceCode"];
        _pollingInterval = TimeSpan.FromSeconds(
            Math.Max(1, configuration.GetValue("Enrollment:RemotePollingIntervalSeconds", 5)));

        bool configuredEnabled = configuration.GetValue("Enrollment:RemotePollingEnabled", true);
        _isEnabled = configuredEnabled
            && !string.IsNullOrWhiteSpace(_pendingEnrollmentUrl)
            && !string.IsNullOrWhiteSpace(_completeEnrollmentUrl)
            && !string.IsNullOrWhiteSpace(_tenantId)
            && !string.IsNullOrWhiteSpace(_deviceCode);

        if (configuredEnabled && !_isEnabled)
        {
            _logger.LogWarning(
                "Enrolamiento remoto deshabilitado. Configure Backend:BaseUrl, Agent:TenantId y Agent:DeviceCode.");
        }
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_isEnabled) return;

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await TryProcessPendingEnrollmentAsync(stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Error procesando enrolamiento biometrico remoto.");
            }

            await _delay(_pollingInterval, stoppingToken);
        }
    }

    internal async Task<bool> TryProcessPendingEnrollmentAsync(CancellationToken cancellationToken)
    {
        if (!_isEnabled) return false;

        using HttpRequestMessage request = new(
            HttpMethod.Get,
            BuildPendingEnrollmentUrl(_pendingEnrollmentUrl!, _deviceCode!));
        AddBackendHeaders(request);

        using HttpResponseMessage response = await _httpClient.SendAsync(request, cancellationToken);
        if (response.StatusCode == System.Net.HttpStatusCode.NoContent)
        {
            return false;
        }

        if (!response.IsSuccessStatusCode)
        {
            _logger.LogWarning(
                "No se pudo consultar enrolamiento pendiente. StatusCode={StatusCode}.",
                (int)response.StatusCode);
            return false;
        }

        string json = await response.Content.ReadAsStringAsync(cancellationToken);
        PendingEnrollSessionResponse? pending = JsonSerializer.Deserialize<PendingEnrollSessionResponse>(json, JsonOptions);
        if (pending is null || string.IsNullOrWhiteSpace(pending.EnrollToken))
        {
            _logger.LogWarning("El backend devolvio una sesion de enrolamiento pendiente invalida.");
            return false;
        }

        if (pending.ExpiresAt is not null && pending.ExpiresAt.Value <= DateTimeOffset.UtcNow)
        {
            _logger.LogWarning(
                "Sesion de enrolamiento pendiente expirada (ExpiresAt={ExpiresAt}). Se ignora.",
                pending.ExpiresAt.Value);
            return false;
        }

        FingerprintEnrollResult enrollResult = await _scannerService.EnrollFingerprintAsync(
            (progress, _) =>
            {
                if (_logger.IsEnabled(LogLevel.Information))
                    _logger.LogInformation(
                        "Enrolamiento remoto: muestra {Sample}/{Total}. {Message}",
                        progress.Step,
                        progress.TotalSteps,
                        progress.Message);
                return Task.CompletedTask;
            },
            cancellationToken,
            // Puebla el padrón local de identificación 1:N con la referencia del usuario del backend.
            userReference: pending.TenantUserId.ToString(System.Globalization.CultureInfo.InvariantCulture),
            fingerIndex: pending.FingerIndex);

        if (!enrollResult.Success || enrollResult.EncryptedRegisteredTemplate is null)
        {
            _logger.LogWarning(
                "No se pudo completar captura de enrolamiento remoto. Mensaje={Message}.",
                enrollResult.Message);
            return false;
        }

        return await CompleteEnrollmentAsync(pending, enrollResult, cancellationToken);
    }

    private async Task<bool> CompleteEnrollmentAsync(
        PendingEnrollSessionResponse pending,
        FingerprintEnrollResult enrollResult,
        CancellationToken cancellationToken)
    {
        EncryptedPayload encrypted = enrollResult.EncryptedRegisteredTemplate!;
        string deviceCode = pending.DeviceCode ?? enrollResult.DeviceId ?? _deviceCode!;

        // El backend (CompleteEnrollRequest.java) espera:
        //   template_cifrado, llave_cifrado, capturado_en (LocalDateTime sin offset),
        //   finger_index, device_code, enroll_token.
        // Empaquetamos iv||cipher||tag dentro de `template_cifrado` para no perder
        // los metadatos AES-GCM (Jackson en el backend lo recibe como string opaco).
        var payload = new
        {
            enroll_token = pending.EnrollToken,
            device_code = deviceCode,
            finger_index = pending.FingerIndex,
            template_cifrado = encrypted.ToPackedTemplateBase64(),
            llave_cifrado = encrypted.EncryptedAesKeyBase64,
            capturado_en = FormatAsLocalDateTime(enrollResult.CapturedAtUtc)
        };

        using StringContent content = new(
            JsonSerializer.Serialize(payload, JsonOptions),
            Encoding.UTF8,
            "application/json");
        using HttpRequestMessage request = new(HttpMethod.Post, _completeEnrollmentUrl!) { Content = content };
        AddBackendHeaders(request);

        using HttpResponseMessage response = await _httpClient.SendAsync(request, cancellationToken);
        if (response.IsSuccessStatusCode)
        {
            if (_logger.IsEnabled(LogLevel.Information))
                _logger.LogInformation("Enrolamiento remoto completado para device_code={DeviceCode}.", deviceCode);
            return true;
        }

        _logger.LogWarning(
            "El backend rechazo el enrolamiento remoto. StatusCode={StatusCode}.",
            (int)response.StatusCode);
        return false;
    }

    private void AddBackendHeaders(HttpRequestMessage request)
    {
        if (!string.IsNullOrWhiteSpace(_agentToken))
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _agentToken);
        if (!string.IsNullOrWhiteSpace(_tenantId))
            request.Headers.Add("X-Tenant-ID", _tenantId);
    }

    internal static string BuildPendingEnrollmentUrl(string endpointUrl, string deviceCode)
    {
        string separator = endpointUrl.Contains('?') ? "&" : "?";
        return $"{endpointUrl}{separator}device_code={Uri.EscapeDataString(deviceCode)}";
    }

    // Jackson en Spring parsea LocalDateTime sin offset. Convertimos a UTC y
    // emitimos ISO 8601 sin zona (los milisegundos son opcionales pero útiles).
    internal static string FormatAsLocalDateTime(DateTimeOffset value)
        => value.UtcDateTime.ToString("yyyy-MM-ddTHH:mm:ss.fff", System.Globalization.CultureInfo.InvariantCulture);

    private sealed class PendingEnrollSessionResponse
    {
        [JsonPropertyName("enroll_token")]
        public string? EnrollToken { get; init; }

        [JsonPropertyName("device_id")]
        public int DeviceId { get; init; }

        [JsonPropertyName("device_code")]
        public string? DeviceCode { get; init; }

        [JsonPropertyName("tenant_user_id")]
        public int TenantUserId { get; init; }

        [JsonPropertyName("finger_index")]
        public int FingerIndex { get; init; }

        [JsonPropertyName("expires_at")]
        public DateTimeOffset? ExpiresAt { get; init; }
    }
}
