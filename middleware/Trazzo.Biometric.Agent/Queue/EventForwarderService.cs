using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using Trazzo.Biometric.Agent.Backend;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Queue;

internal sealed record HttpSenderConfig(
    string BackendUrl,
    string? AgentToken,
    string? TenantId = null,
    string? DeviceCode = null);

public sealed class EventForwarderService : BackgroundService
{
    // 8 KB para respuesta de asistencia (ok/rejected).
    private static readonly HttpClient SharedHttpClient = new()
    {
        Timeout = TimeSpan.FromSeconds(15),
        MaxResponseContentBufferSize = 8 * 1024
    };
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly IEventQueue _queue;
    private readonly Func<BiometricEvent, CancellationToken, Task<bool>> _sender;
    private readonly int _retryIntervalSeconds;
    private readonly bool _isEnabled;
    private readonly ILogger<EventForwarderService> _logger;
    private readonly Func<TimeSpan, CancellationToken, Task> _delay;
    private readonly Func<double> _nextJitter;

    internal EventForwarderService(
        IEventQueue queue,
        Func<BiometricEvent, CancellationToken, Task<bool>> sender,
        int retryIntervalSeconds,
        ILogger<EventForwarderService> logger,
        Func<TimeSpan, CancellationToken, Task>? delay = null,
        Func<double>? nextJitter = null)
    {
        _queue = queue;
        _sender = sender;
        _retryIntervalSeconds = retryIntervalSeconds;
        _logger = logger;
        _isEnabled = true;
        _delay = delay ?? Task.Delay;
        _nextJitter = nextJitter ?? Random.Shared.NextDouble;
    }

    internal EventForwarderService(
        IEventQueue queue,
        HttpClient httpClient,
        HttpSenderConfig config,
        int retryIntervalSeconds,
        ILogger<EventForwarderService> logger)
    {
        _queue = queue;
        _logger = logger;
        _retryIntervalSeconds = retryIntervalSeconds;
        _sender = BuildHttpSender(config.BackendUrl, config.AgentToken, config.TenantId, config.DeviceCode, httpClient, logger);
        _isEnabled = true;
        _delay = Task.Delay;
        _nextJitter = Random.Shared.NextDouble;
    }

    public EventForwarderService(
        IEventQueue queue,
        IConfiguration configuration,
        ILogger<EventForwarderService> logger)
    {
        _queue = queue;
        _logger = logger;
        _retryIntervalSeconds = configuration.GetValue<int>("Queue:RetryIntervalSeconds", 30);

        string? backendUrl = BackendEndpointResolver.EnsureSecureUrl(
            BackendEndpointResolver.ResolveAttendanceSyncUrl(configuration),
            logger,
            "Backend:Endpoints:AttendanceSync");
        string? agentToken = AgentTokenProtector.ResolveAgentToken(configuration, logger);
        string? tenantId = configuration["Agent:TenantId"];
        string? deviceCode = configuration["Agent:DeviceCode"];

        if (string.IsNullOrWhiteSpace(backendUrl))
        {
            _logger.LogWarning(
                "Endpoint de asistencia no configurado. Configure Backend:BaseUrl o Queue:BackendUrl para habilitar el reenvío.");
            _sender = (_, _) => Task.FromResult(false);
            _isEnabled = false;
        }
        else
        {
            _sender = BuildHttpSender(backendUrl, agentToken, tenantId, deviceCode, SharedHttpClient, _logger);
            _isEnabled = true;
        }

        _delay = Task.Delay;
        _nextJitter = Random.Shared.NextDouble;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_isEnabled) return;

        int consecutiveFailures = 0;

        while (!stoppingToken.IsCancellationRequested)
        {
            bool hadFailures = await TryForwardPendingAsync(stoppingToken);
            consecutiveFailures = hadFailures ? consecutiveFailures + 1 : 0;

            // Backoff exponencial: base * 2^(failures-1), máximo 5 minutos, más ±10% de jitter
            double delaySeconds = Math.Min(
                _retryIntervalSeconds * Math.Pow(2, Math.Max(0, consecutiveFailures - 1)),
                300);
            double jitter = delaySeconds * 0.1 * (_nextJitter() * 2 - 1);

            try
            {
                await _delay(TimeSpan.FromSeconds(delaySeconds + jitter), stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }

    internal async Task<bool> TryForwardPendingAsync(CancellationToken cancellationToken)
    {
        bool hadFailures = false;
        try
        {
            IReadOnlyList<BiometricEvent> pending = await _queue.GetPendingAsync(50, cancellationToken);
            if (pending.Count == 0) return false;

            _logger.LogInformation(
                "Cola biométrica: reenviando {Count} evento(s) pendiente(s) al backend.",
                pending.Count);

            List<long> sentIds = [];

            foreach (BiometricEvent evt in pending)
            {
                if (cancellationToken.IsCancellationRequested) break;

                try
                {
                    bool sent = await _sender(evt, cancellationToken);
                    if (sent) sentIds.Add(evt.Id);
                    else
                    {
                        hadFailures = true;
                        await _queue.MarkFailedAsync(evt.Id, cancellationToken);
                    }
                }
                catch (Exception ex) when (!cancellationToken.IsCancellationRequested)
                {
                    hadFailures = true;
                    _logger.LogWarning(ex, "Error al reenviar evento biométrico Id={Id}.", evt.Id);
                    await _queue.MarkFailedAsync(evt.Id, cancellationToken);
                }
            }

            if (sentIds.Count > 0)
            {
                await _queue.MarkSentAsync(sentIds, cancellationToken);
                _logger.LogInformation(
                    "Cola biométrica: {Count} evento(s) reenviado(s) correctamente.",
                    sentIds.Count);
            }

            await _queue.PruneAsync(TimeSpan.FromDays(7), cancellationToken);
        }
        catch (OperationCanceledException)
        {
            throw;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Error al procesar la cola de eventos biométricos.");
            return true;
        }

        return hadFailures;
    }

    private static Func<BiometricEvent, CancellationToken, Task<bool>> BuildHttpSender(
        string backendUrl, string? agentToken, string? tenantId, string? deviceCode,
        HttpClient httpClient, ILogger logger)
    {
        return async (evt, ct) =>
        {
            if (!string.Equals(evt.EventType, "identify", StringComparison.OrdinalIgnoreCase))
            {
                // Retornamos false para que MarkFailedAsync incremente retry_count. Tras MaxRetries pasa a Failed.
                // Antes se marcaba como Sent silenciosamente → data loss oculto.
                logger.LogError(
                    "Evento Id={Id} de tipo '{EventType}' no es soportado por el forwarder de asistencia. Se marcará como fallido (no se descarta).",
                    evt.Id, evt.EventType);
                return false;
            }

            string? resolvedDeviceCode = string.IsNullOrWhiteSpace(deviceCode) ? evt.DeviceId : deviceCode;
            if (string.IsNullOrWhiteSpace(resolvedDeviceCode))
            {
                logger.LogError(
                    "Evento Id={Id} (tipo '{EventType}') no tiene device_code ni en la configuración (Agent:DeviceCode) ni en el evento. Se marcará como fallido.",
                    evt.Id, evt.EventType);
                return false;
            }

            // Nomenclatura alineada con el DTO real del backend (template_cifrado /
            // llave_cifrado / capturado_en). Empaquetamos iv||cipher||tag dentro de
            // template_cifrado. `offline_event_id`, `created_at_utc` y `retry_count`
            // son campos de trazabilidad de la cola offline — el backend los ignora
            // si no los conoce (Jackson default: FAIL_ON_UNKNOWN_PROPERTIES=false).
            byte[] iv = Convert.FromBase64String(evt.IvBase64);
            byte[] cipher = Convert.FromBase64String(evt.EncryptedTemplateBase64);
            byte[] tag = Convert.FromBase64String(evt.TagBase64);
            byte[] packed = new byte[iv.Length + cipher.Length + tag.Length];
            Buffer.BlockCopy(iv, 0, packed, 0, iv.Length);
            Buffer.BlockCopy(cipher, 0, packed, iv.Length, cipher.Length);
            Buffer.BlockCopy(tag, 0, packed, iv.Length + cipher.Length, tag.Length);

            var payload = new
            {
                event_type = "identify",
                template_cifrado = Convert.ToBase64String(packed),
                llave_cifrado = evt.EncryptedAesKeyBase64,
                capturado_en = evt.CapturedAtUtc.UtcDateTime.ToString(
                    "yyyy-MM-ddTHH:mm:ss.fff",
                    System.Globalization.CultureInfo.InvariantCulture),
                device_code = resolvedDeviceCode,
                offline_event_id = evt.Id,
                retry_count = evt.RetryCount
            };

            using StringContent content = new(
                JsonSerializer.Serialize(new[] { payload }, JsonOptions),
                Encoding.UTF8,
                "application/json");

            using HttpRequestMessage request = new(HttpMethod.Post, backendUrl) { Content = content };
            if (!string.IsNullOrWhiteSpace(agentToken))
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", agentToken);
            if (!string.IsNullOrWhiteSpace(tenantId))
                request.Headers.Add("X-Tenant-ID", tenantId);

            using HttpResponseMessage response = await httpClient.SendAsync(request, ct);
            if (!response.IsSuccessStatusCode) return false;

            await TryLogCorrelationIdAsync(response, evt.Id, logger, ct);
            return true;
        };
    }

    // Extrae correlation_id de MarcacionSyncResponse para trazabilidad en soporte.
    // Best-effort: si el body no es JSON válido o no está el campo, no rompe el envío.
    private static async Task TryLogCorrelationIdAsync(
        HttpResponseMessage response,
        long eventId,
        ILogger logger,
        CancellationToken ct)
    {
        try
        {
            if (response.Content is null) return;
            string body = await response.Content.ReadAsStringAsync(ct);
            if (string.IsNullOrWhiteSpace(body)) return;

            using JsonDocument doc = JsonDocument.Parse(body);
            if (doc.RootElement.ValueKind != JsonValueKind.Object) return;

            string? correlationId = doc.RootElement.TryGetProperty("correlation_id", out JsonElement corr)
                ? corr.GetString()
                : null;
            int? acceptedCount = doc.RootElement.TryGetProperty("accepted_count", out JsonElement acc)
                                 && acc.ValueKind == JsonValueKind.Number
                ? acc.GetInt32()
                : null;

            if (!string.IsNullOrWhiteSpace(correlationId) && logger.IsEnabled(LogLevel.Information))
                logger.LogInformation(
                    "Sync aceptado por backend. EventId={EventId}, CorrelationId={CorrelationId}, AcceptedCount={AcceptedCount}.",
                    eventId, correlationId, acceptedCount);
        }
        catch (JsonException)
        {
            // Backend puede responder cuerpo vacío en 202/200. Silencioso.
        }
        catch (Exception ex)
        {
            logger.LogDebug(ex, "No se pudo leer el correlation_id de la respuesta de /asistencia/sync.");
        }
    }
}
