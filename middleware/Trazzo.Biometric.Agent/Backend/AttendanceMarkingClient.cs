using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Security;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.Backend;

public sealed class AttendanceMarkingClient : IAttendanceMarkingClient
{
    // 8 KB alcanza para respuesta de marcación (ok / rejected + metadata).
    private static readonly HttpClient SharedHttpClient = new()
    {
        Timeout = TimeSpan.FromSeconds(15),
        MaxResponseContentBufferSize = 8 * 1024
    };
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly HttpClient _httpClient;
    private readonly ILogger<AttendanceMarkingClient> _logger;
    private readonly string? _attendanceMarkUrl;
    private readonly string? _agentToken;
    private readonly string? _tenantId;
    private readonly string? _deviceCode;
    private readonly bool _isEnabled;

    public AttendanceMarkingClient(
        IConfiguration configuration,
        ILogger<AttendanceMarkingClient> logger)
        : this(configuration, logger, SharedHttpClient)
    {
    }

    internal AttendanceMarkingClient(
        IConfiguration configuration,
        ILogger<AttendanceMarkingClient> logger,
        HttpClient httpClient)
    {
        _logger = logger;
        _httpClient = httpClient;
        _attendanceMarkUrl = BackendEndpointResolver.EnsureSecureUrl(
            BackendEndpointResolver.ResolveAttendanceMarkUrl(configuration),
            logger,
            "Backend:Endpoints:AttendanceMark");
        _agentToken = AgentTokenProtector.ResolveAgentToken(configuration, logger);
        _tenantId = configuration["Agent:TenantId"];
        _deviceCode = configuration["Agent:DeviceCode"];

        _isEnabled = !string.IsNullOrWhiteSpace(_attendanceMarkUrl)
            && !string.IsNullOrWhiteSpace(_tenantId);

        if (!_isEnabled)
        {
            _logger.LogInformation(
                "Marcacion biometrica sincrona deshabilitada. Configure Backend:BaseUrl y Agent:TenantId para usar /asistencia/marcar.");
        }
    }

    public async Task<bool> TryMarkAsync(
        EncryptedPayload encryptedTemplate,
        string? deviceId,
        DateTimeOffset capturedAtUtc,
        CancellationToken cancellationToken)
    {
        if (!_isEnabled)
        {
            return false;
        }

        string? resolvedDeviceCode = string.IsNullOrWhiteSpace(_deviceCode) ? deviceId : _deviceCode;
        if (string.IsNullOrWhiteSpace(resolvedDeviceCode))
        {
            _logger.LogWarning("No se envio marcacion sincrona porque falta Agent:DeviceCode o deviceId del lector.");
            return false;
        }

        // Nomenclatura alineada con lo que el backend ya expone en enroll/completar
        // (template_cifrado / llave_cifrado / capturado_en). El iv+tag AES-GCM van
        // empaquetados dentro de template_cifrado (formato estándar iv||cipher||tag).
        var payload = new
        {
            event_type = "identify",
            template_cifrado = encryptedTemplate.ToPackedTemplateBase64(),
            llave_cifrado = encryptedTemplate.EncryptedAesKeyBase64,
            capturado_en = RemoteEnrollmentService.FormatAsLocalDateTime(capturedAtUtc),
            device_code = resolvedDeviceCode
        };

        using StringContent content = new(
            JsonSerializer.Serialize(payload, JsonOptions),
            Encoding.UTF8,
            "application/json");
        using HttpRequestMessage request = new(HttpMethod.Post, _attendanceMarkUrl!) { Content = content };
        AddBackendHeaders(request);

        try
        {
            using HttpResponseMessage response = await _httpClient.SendAsync(request, cancellationToken);
            if (response.IsSuccessStatusCode)
            {
                return true;
            }

            _logger.LogWarning(
                "El backend rechazo la marcacion sincrona. StatusCode={StatusCode}.",
                (int)response.StatusCode);
            return false;
        }
        catch (Exception ex) when (ex is not OperationCanceledException || !cancellationToken.IsCancellationRequested)
        {
            _logger.LogWarning(ex, "No se pudo enviar marcacion sincrona al backend.");
            return false;
        }
    }

    private void AddBackendHeaders(HttpRequestMessage request)
    {
        if (!string.IsNullOrWhiteSpace(_agentToken))
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _agentToken);
        if (!string.IsNullOrWhiteSpace(_tenantId))
            request.Headers.Add("X-Tenant-ID", _tenantId);
    }
}
