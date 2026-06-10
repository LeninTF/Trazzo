using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;

namespace Trazzo.Biometric.Agent.Queue;

public sealed class EventForwarderService : BackgroundService
{
    private static readonly HttpClient SharedHttpClient = new() { Timeout = TimeSpan.FromSeconds(15) };
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
        string backendUrl,
        string? agentToken,
        int retryIntervalSeconds,
        ILogger<EventForwarderService> logger,
        string? tenantId = null)
    {
        _queue = queue;
        _logger = logger;
        _retryIntervalSeconds = retryIntervalSeconds;
        _sender = BuildHttpSender(backendUrl, agentToken, tenantId, httpClient);
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

        string? backendUrl = configuration["Queue:BackendUrl"];
        string? agentToken = configuration["Queue:AgentToken"];
        string? tenantId = configuration["Agent:TenantId"];

        if (string.IsNullOrWhiteSpace(backendUrl))
        {
            _logger.LogWarning(
                "Queue:BackendUrl no configurada. El reenvío automático de eventos biométricos está deshabilitado.");
            _sender = (_, _) => Task.FromResult(false);
            _isEnabled = false;
        }
        else
        {
            _sender = BuildHttpSender(backendUrl, agentToken, tenantId, SharedHttpClient);
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

    // Retorna true si hubo errores de envío al backend; false si la cola estaba vacía o todo se envió.
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
        string backendUrl, string? agentToken, string? tenantId, HttpClient httpClient)
    {
        return async (evt, ct) =>
        {
            // template = base64( iv[12] || tag[16] || ciphertext[N] )
            byte[] iv = Convert.FromBase64String(evt.IvBase64);
            byte[] tag = Convert.FromBase64String(evt.TagBase64);
            byte[] ciphertext = Convert.FromBase64String(evt.EncryptedTemplateBase64);
            byte[] combined = new byte[iv.Length + tag.Length + ciphertext.Length];
            Buffer.BlockCopy(iv, 0, combined, 0, iv.Length);
            Buffer.BlockCopy(tag, 0, combined, iv.Length, tag.Length);
            Buffer.BlockCopy(ciphertext, 0, combined, iv.Length + tag.Length, ciphertext.Length);

            var payload = new
            {
                templateCifrado = Convert.ToBase64String(combined),
                llaveCifrada = evt.EncryptedAesKeyBase64,
                timestampLocal = evt.CapturedAtUtc.ToString("O"),
                dispositivoId = evt.DeviceId
            };

            using StringContent content = new(
                JsonSerializer.Serialize(payload, JsonOptions),
                Encoding.UTF8,
                "application/json");

            using HttpRequestMessage request = new(HttpMethod.Post, backendUrl) { Content = content };
            if (!string.IsNullOrWhiteSpace(agentToken))
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", agentToken);
            if (!string.IsNullOrWhiteSpace(tenantId))
                request.Headers.Add("X-Tenant-ID", tenantId);

            using HttpResponseMessage response = await httpClient.SendAsync(request, ct);
            return response.IsSuccessStatusCode;
        };
    }
}
