using System.Collections.Concurrent;
using System.Diagnostics;
using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Queue;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.WebSocket;

public sealed class LocalWebSocketServerService(
    IBiometricScannerService scannerService,
    IAgentHealthService healthService,
    IEventQueue eventQueue,
    IConfiguration configuration,
    ILogger<LocalWebSocketServerService> logger) : IWebSocketServerService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly HttpListener _listener = new();
    private readonly ConcurrentDictionary<string, DateTimeOffset> _clientLastOperationTime = new();
    private readonly ConcurrentDictionary<string, (System.Net.WebSockets.WebSocket WebSocket, SemaphoreSlim SendLock)> _activeClients = new();
    private readonly int _rateLimitSeconds = configuration.GetValue("Agent:RateLimitSeconds", 5);
    private readonly TimeSpan _keepAliveInterval = TimeSpan.FromSeconds(
        Math.Max(5, configuration.GetValue("Agent:KeepAliveIntervalSeconds", 30)));
    private readonly int _deviceMonitorIntervalSeconds = Math.Max(1,
        configuration.GetValue("Agent:DeviceMonitorIntervalSeconds", 3));
    private CancellationTokenSource? _serverCts;
    private Task? _serverTask;

    public Task StartAsync(CancellationToken cancellationToken)
    {
        string url = configuration["Agent:WebSocketUrl"] ?? "http://localhost:9001/";
        if (!url.EndsWith('/'))
        {
            url += "/";
        }

        _listener.Prefixes.Clear();
        _listener.Prefixes.Add(url);
        _listener.Start();

        _serverCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _serverTask = Task.WhenAll(
            AcceptLoopAsync(_serverCts.Token),
            DeviceMonitorLoopAsync(_serverCts.Token));

        logger.LogInformation("WebSocket escuchando en {Url}", url.Replace("http://", "ws://"));
        return _serverTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        if (_serverCts is not null)
        {
            await _serverCts.CancelAsync();
        }

        if (_listener.IsListening)
        {
            _listener.Stop();
        }

        if (_serverTask is not null)
        {
            await Task.WhenAny(_serverTask, Task.Delay(TimeSpan.FromSeconds(5), cancellationToken));
        }

        _listener.Close();
        _serverCts?.Dispose();
    }

    private async Task AcceptLoopAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            HttpListenerContext context;

            try
            {
                context = await _listener.GetContextAsync().WaitAsync(cancellationToken);
            }
            catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
            {
                break;
            }
            catch (HttpListenerException) when (!_listener.IsListening)
            {
                break;
            }

            StartContextHandler(context, cancellationToken);
        }
    }

    private async Task HandleContextAsync(HttpListenerContext context, CancellationToken cancellationToken)
    {
        if (!context.Request.IsWebSocketRequest)
        {
            if (context.Request.HttpMethod == "GET" &&
                context.Request.Url?.AbsolutePath is "/" or "/health")
            {
                byte[] body = JsonSerializer.SerializeToUtf8Bytes(
                    healthService.GetHealthResult(), JsonOptions);
                context.Response.StatusCode = 200;
                context.Response.ContentType = "application/json; charset=utf-8";
                context.Response.ContentLength64 = body.Length;
                await context.Response.OutputStream.WriteAsync(body, cancellationToken);
            }
            else
            {
                context.Response.StatusCode = 400;
            }

            context.Response.Close();
            return;
        }

        string[] allowedOrigins = configuration.GetSection("Agent:AllowedOrigins").Get<string[]>() ?? [];
        if (allowedOrigins.Length > 0)
        {
            string? origin = context.Request.Headers["Origin"];
            if (!Array.Exists(allowedOrigins, o => string.Equals(o, origin, StringComparison.OrdinalIgnoreCase)))
            {
                logger.LogWarning("Conexión WebSocket rechazada. Origen no permitido: {Origin}", origin);
                context.Response.StatusCode = 403;
                context.Response.Close();
                return;
            }
        }

        using System.Net.WebSockets.WebSocket webSocket = (await context.AcceptWebSocketAsync(subProtocol: null, _keepAliveInterval)).WebSocket;
        string clientId = context.Request.RemoteEndPoint?.ToString() ?? "unknown";
        logger.LogInformation("Cliente WebSocket conectado desde {ClientId}", clientId);

        await ReceiveLoopAsync(webSocket, clientId, cancellationToken);
    }

    private async Task ReceiveLoopAsync(System.Net.WebSockets.WebSocket webSocket, string clientId, CancellationToken cancellationToken)
    {
        Stopwatch sessionTimer = Stopwatch.StartNew();
        byte[] buffer = new byte[8192];
        SemaphoreSlim sendLock = new(1, 1);
        List<Task> pendingOperations = [];

        _activeClients[clientId] = (webSocket, sendLock);

        try
        {
            while (webSocket.State == WebSocketState.Open && !cancellationToken.IsCancellationRequested)
            {
                using MemoryStream messageStream = new();
                WebSocketReceiveResult receiveResult;

                do
                {
                    receiveResult = await webSocket.ReceiveAsync(buffer, cancellationToken);
                    if (receiveResult.MessageType == WebSocketMessageType.Close)
                    {
                        await webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "El cliente cerró la conexión.", cancellationToken);
                        return;
                    }

                    messageStream.Write(buffer, 0, receiveResult.Count);
                }
                while (!receiveResult.EndOfMessage);

                string json = Encoding.UTF8.GetString(messageStream.ToArray());
                pendingOperations.RemoveAll(task => task.IsCompleted);
                pendingOperations.Add(Task.Run(
                    () => ProcessClientMessageAsync(json, webSocket, clientId, sendLock, cancellationToken),
                    CancellationToken.None));
            }
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
        }
        catch (WebSocketException)
        {
        }
        finally
        {
            _activeClients.TryRemove(clientId, out _);
            sendLock.Dispose();
            _clientLastOperationTime.TryRemove(clientId, out _);
            logger.LogInformation(
                "Cliente WebSocket desconectado: {ClientId}. Duracion de sesion: {Duration}s.",
                clientId, sessionTimer.Elapsed.TotalSeconds.ToString("F1"));
        }
    }

    private void StartContextHandler(HttpListenerContext context, CancellationToken cancellationToken)
    {
        Task.Run(() => HandleContextAsync(context, cancellationToken), cancellationToken)
            .ContinueWith(
                t => logger.LogDebug(t.Exception?.GetBaseException(), "Error no controlado en handler de contexto WebSocket."),
                CancellationToken.None,
                TaskContinuationOptions.OnlyOnFaulted,
                TaskScheduler.Default);
    }

    private async Task DeviceMonitorLoopAsync(CancellationToken cancellationToken)
    {
        bool? lastKnownConnected = null;
        DateTimeOffset? disconnectedSince = null;
        TimeSpan pollInterval = TimeSpan.FromSeconds(_deviceMonitorIntervalSeconds);

        while (!cancellationToken.IsCancellationRequested)
        {
            try
            {
                FingerprintDeviceStatus status = await scannerService.GetStatusAsync(cancellationToken);
                bool isConnected = status.IsConnected;

                if (lastKnownConnected is null)
                {
                    // Primera lectura: establece línea base sin notificar
                    lastKnownConnected = isConnected;
                    if (!isConnected)
                        disconnectedSince = DateTimeOffset.UtcNow;
                }
                else if (isConnected && lastKnownConnected == false)
                {
                    // Lector recién conectado
                    lastKnownConnected = true;
                    disconnectedSince = null;
                    logger.LogInformation("Lector biométrico conectado. Notificando clientes.");
                    await BroadcastAsync(new
                    {
                        type = "device.status.changed",
                        success = true,
                        isConnected = true,
                        message = "Detector de huella conectado."
                    }, cancellationToken);
                }
                else if (!isConnected && lastKnownConnected == true)
                {
                    // Lector recién desconectado
                    lastKnownConnected = false;
                    disconnectedSince = DateTimeOffset.UtcNow;
                    logger.LogWarning("Lector biométrico desconectado. Notificando clientes.");
                    await BroadcastAsync(new
                    {
                        type = "device.status.changed",
                        success = false,
                        isConnected = false,
                        message = "Lector biométrico desconectado."
                    }, cancellationToken);
                }
                else if (!isConnected && disconnectedSince is not null)
                {
                    // Sigue sin lector: enviar tiempo de espera a clientes activos
                    int waitingSeconds = (int)(DateTimeOffset.UtcNow - disconnectedSince.Value).TotalSeconds;
                    await BroadcastAsync(new
                    {
                        type = "device.connecting",
                        success = false,
                        isConnected = false,
                        message = $"Buscando lector biométrico... ({waitingSeconds}s)",
                        waitingSeconds
                    }, cancellationToken);
                }

                await Task.Delay(pollInterval, cancellationToken);
            }
            catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                logger.LogDebug(ex, "Error en monitor de dispositivo biométrico.");
                try { await Task.Delay(pollInterval, cancellationToken); }
                catch (OperationCanceledException) { break; }
            }
        }
    }

    private async Task BroadcastAsync(object message, CancellationToken cancellationToken)
    {
        if (_activeClients.IsEmpty) return;

        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(message, JsonOptions);

        foreach ((string clientId, (System.Net.WebSockets.WebSocket ws, SemaphoreSlim sendLock)) in _activeClients)
        {
            try
            {
                await sendLock.WaitAsync(cancellationToken);
                try
                {
                    if (ws.State == WebSocketState.Open)
                        await ws.SendAsync(payload, WebSocketMessageType.Text, endOfMessage: true, cancellationToken);
                }
                finally
                {
                    sendLock.Release();
                }
            }
            catch (ObjectDisposedException)
            {
                // El cliente se desconectó justo mientras se enviaba la notificación
            }
            catch (Exception ex) when (!cancellationToken.IsCancellationRequested)
            {
                logger.LogDebug(ex, "No se pudo notificar al cliente {ClientId}.", clientId);
            }
        }
    }

    internal Task<object> HandleMessageAsync(string json, CancellationToken cancellationToken)
    {
        return HandleMessageAsync(json, static (_, _) => Task.CompletedTask, cancellationToken);
    }

    private async Task ProcessClientMessageAsync(
        string json,
        System.Net.WebSockets.WebSocket webSocket,
        string clientId,
        SemaphoreSlim sendLock,
        CancellationToken cancellationToken)
    {
        try
        {
            string? operationType = TryParseMessageType(json);
            bool isBiometricOp = operationType is "fingerprint.capture" or "fingerprint.identify" or "fingerprint.enroll.start";

            if (isBiometricOp && IsRateLimited(clientId, operationType!))
            {
                await SendJsonAsync(webSocket,
                    new { type = "error", success = false, message = $"Demasiadas solicitudes. Espere {_rateLimitSeconds}s entre operaciones biometricas." },
                    sendLock, cancellationToken);
                return;
            }

            Stopwatch opTimer = Stopwatch.StartNew();
            bool isEnrollStart = operationType == "fingerprint.enroll.start";
            object response = await HandleMessageAsync(
                json,
                (progress, token) => SendJsonAsync(webSocket, progress, sendLock, token),
                cancellationToken);

            if (isBiometricOp)
                logger.LogInformation(
                    "Operacion {OperationType} de {ClientId} completada en {ElapsedMs}ms.",
                    operationType, clientId, opTimer.ElapsedMilliseconds);

            if (isEnrollStart && response is FingerprintEnrollResult { Success: false, Message: "Enrolamiento cancelado." })
            {
                return;
            }

            await SendJsonAsync(webSocket, response, sendLock, cancellationToken);
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
        }
        catch (WebSocketException ex)
        {
            logger.LogDebug(ex, "No se pudo enviar la respuesta WebSocket porque el cliente ya no está conectado.");
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Error procesando mensaje WebSocket.");
            if (webSocket.State == WebSocketState.Open)
            {
                await SendJsonAsync(
                    webSocket,
                    new { type = "error", success = false, message = "Error interno procesando el mensaje WebSocket." },
                    sendLock,
                    cancellationToken);
            }
        }
    }

    private bool IsRateLimited(string clientId, string operationType)
    {
        if (_rateLimitSeconds <= 0) return false;

        DateTimeOffset now = DateTimeOffset.UtcNow;
        if (_clientLastOperationTime.TryGetValue(clientId, out DateTimeOffset last)
            && (now - last).TotalSeconds < _rateLimitSeconds)
        {
            double remaining = _rateLimitSeconds - (now - last).TotalSeconds;
            logger.LogWarning(
                "Rate limit: {OperationType} rechazada para {ClientId}. Espere {Remaining:F0}s.",
                operationType, clientId, remaining);
            return true;
        }

        _clientLastOperationTime[clientId] = now;
        return false;
    }

    private static string? TryParseMessageType(string json)
    {
        try
        {
            WebSocketMessage? message = JsonSerializer.Deserialize<WebSocketMessage>(json, JsonOptions);
            return message?.Type;
        }
        catch (JsonException)
        {
            return null;
        }
    }

    private async Task<object> HandleMessageAsync(
        string json,
        Func<FingerprintEnrollProgress, CancellationToken, Task> enrollmentProgressCallback,
        CancellationToken cancellationToken)
    {
        try
        {
            WebSocketMessage? message = JsonSerializer.Deserialize<WebSocketMessage>(json, JsonOptions);
            return message?.Type switch
            {
                "health.check" => healthService.GetHealthResult(),
                "device.status" => await scannerService.GetStatusAsync(cancellationToken),
                "fingerprint.capture" => await CaptureAndEnqueueAsync(cancellationToken),
                "fingerprint.identify" => await IdentifyAndEnqueueAsync(cancellationToken),
                "fingerprint.enroll.start" => await EnrollAndEnqueueAsync(enrollmentProgressCallback, cancellationToken),
                "fingerprint.enroll.cancel" => scannerService.CancelEnrollment(),
                "queue.status" => new
                {
                    type = "queue.status.result",
                    success = true,
                    pendingCount = await eventQueue.GetPendingCountAsync(cancellationToken)
                },
                _ => new
                {
                    type = "error",
                    success = false,
                    message = "Tipo de mensaje WebSocket no soportado."
                }
            };
        }
        catch (JsonException)
        {
            return new
            {
                type = "error",
                success = false,
                message = "Mensaje JSON inválido."
            };
        }
    }

    private static async Task SendJsonAsync(
        System.Net.WebSockets.WebSocket webSocket,
        object response,
        SemaphoreSlim sendLock,
        CancellationToken cancellationToken)
    {
        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(response, JsonOptions);

        await sendLock.WaitAsync(cancellationToken);
        try
        {
            if (webSocket.State == WebSocketState.Open)
            {
                await webSocket.SendAsync(payload, WebSocketMessageType.Text, endOfMessage: true, cancellationToken);
            }
        }
        finally
        {
            sendLock.Release();
        }
    }

    private async Task<FingerprintCaptureResult> CaptureAndEnqueueAsync(CancellationToken ct)
    {
        FingerprintCaptureResult result = await scannerService.CaptureFingerprintAsync(ct);
        await TryEnqueueAsync(result.EncryptedTemplate, result.DeviceId, result.CapturedAtUtc, "capture", ct);
        return result;
    }

    private async Task<FingerprintIdentifyResult> IdentifyAndEnqueueAsync(CancellationToken ct)
    {
        FingerprintIdentifyResult result = await scannerService.IdentifyFingerprintAsync(ct);
        await TryEnqueueAsync(result.EncryptedTemplate, result.DeviceId, result.CapturedAtUtc, "identify", ct);
        return result;
    }

    private async Task<FingerprintEnrollResult> EnrollAndEnqueueAsync(
        Func<FingerprintEnrollProgress, CancellationToken, Task> callback, CancellationToken ct)
    {
        FingerprintEnrollResult result = await scannerService.EnrollFingerprintAsync(callback, ct);
        await TryEnqueueAsync(result.EncryptedRegisteredTemplate, result.DeviceId, result.CapturedAtUtc, "enroll", ct);
        return result;
    }

    private async Task TryEnqueueAsync(
        EncryptedPayload? enc, string? deviceId, DateTimeOffset capturedAt, string eventType, CancellationToken ct)
    {
        if (enc is null) return;

        try
        {
            await eventQueue.EnqueueAsync(new BiometricEvent
            {
                EventType = eventType,
                EncryptedTemplateBase64 = enc.EncryptedTemplateBase64,
                EncryptedAesKeyBase64 = enc.EncryptedAesKeyBase64,
                IvBase64 = enc.IvBase64,
                TagBase64 = enc.TagBase64,
                DeviceId = deviceId,
                CapturedAtUtc = capturedAt
            }, ct);
        }
        catch (Exception ex)
        {
            logger.LogWarning(ex, "No se pudo encolar el evento biométrico de tipo {EventType}.", eventType);
        }
    }
}
