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

    internal EventForwarderService(
        IEventQueue queue,
        Func<BiometricEvent, CancellationToken, Task<bool>> sender,
        int retryIntervalSeconds,
        ILogger<EventForwarderService> logger)
    {
        _queue = queue;
        _sender = sender;
        _retryIntervalSeconds = retryIntervalSeconds;
        _logger = logger;
        _isEnabled = true;
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

        if (string.IsNullOrWhiteSpace(backendUrl))
        {
            _logger.LogWarning(
                "Queue:BackendUrl no configurada. El reenvío automático de eventos biométricos está deshabilitado.");
            _sender = (_, _) => Task.FromResult(false);
            _isEnabled = false;
        }
        else
        {
            _sender = BuildHttpSender(backendUrl);
            _isEnabled = true;
        }
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_isEnabled) return;

        while (!stoppingToken.IsCancellationRequested)
        {
            await TryForwardPendingAsync(stoppingToken);

            try
            {
                await Task.Delay(TimeSpan.FromSeconds(_retryIntervalSeconds), stoppingToken);
            }
            catch (OperationCanceledException)
            {
                break;
            }
        }
    }

    internal async Task TryForwardPendingAsync(CancellationToken cancellationToken)
    {
        try
        {
            IReadOnlyList<BiometricEvent> pending = await _queue.GetPendingAsync(50, cancellationToken);
            if (pending.Count == 0) return;

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
                    else await _queue.MarkFailedAsync(evt.Id, cancellationToken);
                }
                catch (Exception ex) when (!cancellationToken.IsCancellationRequested)
                {
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
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Error al procesar la cola de eventos biométricos.");
        }
    }

    private static Func<BiometricEvent, CancellationToken, Task<bool>> BuildHttpSender(string backendUrl)
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

            using HttpResponseMessage response = await SharedHttpClient.PostAsync(backendUrl, content, ct);
            return response.IsSuccessStatusCode;
        };
    }
}
