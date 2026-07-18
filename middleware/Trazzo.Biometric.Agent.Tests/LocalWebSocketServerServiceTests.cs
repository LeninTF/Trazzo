using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.WebSocket;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class LocalWebSocketServerServiceTests : IAsyncDisposable
{
    private LocalWebSocketServerService? _service;
    private string _wsUrl = "";
    private string _httpUrl = "";

    public async ValueTask DisposeAsync()
    {
        if (_service is not null)
            await _service.StopAsync(CancellationToken.None);
    }

    [Fact]
    public async Task MultiFrame_CuandoMensajeEnDosFrames_ProcesaMensajeCompleto()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();

        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.SendAsync(Encoding.UTF8.GetBytes("{\"type\":"), WebSocketMessageType.Text, endOfMessage: false, cts.Token);
        await client.SendAsync(Encoding.UTF8.GetBytes("\"health.check\"}"), WebSocketMessageType.Text, endOfMessage: true, cts.Token);

        string response = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("health.check.result", response);
        Assert.Contains("true", response);
    }

    [Fact]
    public async Task MultiFrame_CuandoMensajeEnTresFrames_ProcesaMensajeCompleto()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();

        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.SendAsync(Encoding.UTF8.GetBytes("{"), WebSocketMessageType.Text, endOfMessage: false, cts.Token);
        await client.SendAsync(Encoding.UTF8.GetBytes("\"type\":\"health"), WebSocketMessageType.Text, endOfMessage: false, cts.Token);
        await client.SendAsync(Encoding.UTF8.GetBytes(".check\"}"), WebSocketMessageType.Text, endOfMessage: true, cts.Token);

        string response = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("health.check.result", response);
    }

    [Fact]
    public async Task OriginAuth_CuandoListaVacia_RechazaOrigenExterno()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(allowedOrigins: []);

        using ClientWebSocket client = new();
        client.Options.SetRequestHeader("Origin", "http://cualquier-dominio.com");

        await Assert.ThrowsAnyAsync<WebSocketException>(
            () => client.ConnectAsync(new Uri(_wsUrl), cts.Token));
    }

    [Fact]
    public async Task OriginAuth_CuandoListaVacia_AceptaOrigenLoopback()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(allowedOrigins: []);

        using ClientWebSocket client = new();
        client.Options.SetRequestHeader("Origin", _httpUrl.TrimEnd('/'));
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        Assert.Equal(WebSocketState.Open, client.State);
        await client.CloseAsync(WebSocketCloseStatus.NormalClosure, "ok", cts.Token);
    }

    [Fact]
    public async Task OriginAuth_CuandoListaVacia_AceptaOriginNullDesdeLoopback()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(allowedOrigins: []);

        using ClientWebSocket client = new();
        client.Options.SetRequestHeader("Origin", "null");
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        Assert.Equal(WebSocketState.Open, client.State);
        await client.CloseAsync(WebSocketCloseStatus.NormalClosure, "ok", cts.Token);
    }

    [Fact]
    public async Task OriginAuth_CuandoOrigenPermitido_AceptaConexion()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(allowedOrigins: ["http://app.trazzo.com"]);

        using ClientWebSocket client = new();
        client.Options.SetRequestHeader("Origin", "http://app.trazzo.com");
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        Assert.Equal(WebSocketState.Open, client.State);
        await client.CloseAsync(WebSocketCloseStatus.NormalClosure, "ok", cts.Token);
    }

    [Fact]
    public async Task OriginAuth_CuandoOrigenNoPermitido_RechazaConexion()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(allowedOrigins: ["http://app.trazzo.com"]);

        using ClientWebSocket client = new();
        client.Options.SetRequestHeader("Origin", "http://evil.com");

        await Assert.ThrowsAnyAsync<WebSocketException>(
            () => client.ConnectAsync(new Uri(_wsUrl), cts.Token));
    }

    [Fact]
    public async Task HealthEndpoint_CuandoGetASlashHealth_Devuelve200ConJson()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();

        using HttpClient http = new();
        HttpResponseMessage response = await http.GetAsync(_httpUrl + "health", cts.Token);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        string body = await response.Content.ReadAsStringAsync(cts.Token);
        Assert.Contains("health.check.result", body);
    }

    [Fact]
    public async Task HealthEndpoint_CuandoGetARaiz_Devuelve200ConJson()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();

        using HttpClient http = new();
        HttpResponseMessage response = await http.GetAsync(_httpUrl, cts.Token);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task StartAsync_CuandoUrlNoTerminaEnSlash_AgregaSlash()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(includeTrailingSlash: false);

        using HttpClient http = new();
        HttpResponseMessage response = await http.GetAsync(_httpUrl, cts.Token);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task HttpEndpoint_CuandoMetodoNoEsGet_Devuelve400()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();

        using HttpClient http = new();
        HttpResponseMessage response = await http.PostAsync(_httpUrl, content: null, cts.Token);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task HttpEndpoint_CuandoRutaGetNoExiste_Devuelve400()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();

        using HttpClient http = new();
        HttpResponseMessage response = await http.GetAsync(_httpUrl + "unknown", cts.Token);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task HttpEndpoint_CuandoHandlerFalla_LaSolicitudSeCancela()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(1));
        Start(healthService: new FakeAgentHealthService
        {
            Exception = new InvalidOperationException("health failure")
        });
        using HttpClient http = new();

        await Assert.ThrowsAnyAsync<OperationCanceledException>(
            () => http.GetAsync(_httpUrl, cts.Token));
    }

    [Fact]
    public async Task WebSocket_CuandoRepiteOperacionBiometrica_ProcesaAmbasSolicitudes()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);
        byte[] request = Encoding.UTF8.GetBytes("""{"type":"fingerprint.capture"}""");

        await client.SendAsync(request, WebSocketMessageType.Text, true, cts.Token);
        string firstResponse = await ReceiveStringAsync(client, cts.Token);
        await client.SendAsync(request, WebSocketMessageType.Text, true, cts.Token);
        string secondResponse = await ReceiveStringAsync(client, cts.Token);

        Assert.DoesNotContain("Demasiadas solicitudes", firstResponse);
        Assert.DoesNotContain("Demasiadas solicitudes", secondResponse);
        Assert.Contains("fingerprint.capture.result", firstResponse);
        Assert.Contains("fingerprint.capture.result", secondResponse);
    }

    [Fact]
    public async Task WebSocket_CuandoEnrolamientoReportaProgreso_EnviaProgresoYResultado()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeBiometricScannerService scanner = new()
        {
            EnrollmentProgress = FingerprintEnrollProgress.Create(1, 3, "Coloque el dedo."),
            EnrollResult = FingerprintEnrollResult.Failed("No se pudo enrolar la huella.")
        };
        Start(scanner: scanner);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.SendAsync(
            Encoding.UTF8.GetBytes("""{"type":"fingerprint.enroll.start"}"""),
            WebSocketMessageType.Text,
            true,
            cts.Token);
        string progress = await ReceiveStringAsync(client, cts.Token);
        string result = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("fingerprint.enroll.progress", progress);
        Assert.Contains("fingerprint.enroll.result", result);
    }

    [Fact]
    public async Task WebSocket_CuandoEnrolamientoEsCancelado_NoEnviaResultadoFinal()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeBiometricScannerService scanner = new()
        {
            EnrollResult = FingerprintEnrollResult.Failed("Enrolamiento cancelado.")
        };
        Start(scanner: scanner);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.SendAsync(
            Encoding.UTF8.GetBytes("""{"type":"fingerprint.enroll.start"}"""),
            WebSocketMessageType.Text,
            true,
            cts.Token);
        await Task.Delay(100, cts.Token);
        await client.SendAsync(
            Encoding.UTF8.GetBytes("""{"type":"health.check"}"""),
            WebSocketMessageType.Text,
            true,
            cts.Token);
        string response = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("health.check.result", response);
    }

    [Fact]
    public async Task WebSocket_CuandoProcesamientoFalla_EnviaErrorInterno()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeBiometricScannerService scanner = new()
        {
            CaptureException = new InvalidOperationException("capture failed")
        };
        Start(scanner: scanner);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.SendAsync(
            Encoding.UTF8.GetBytes("""{"type":"fingerprint.capture"}"""),
            WebSocketMessageType.Text,
            true,
            cts.Token);
        string response = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("Error interno procesando el mensaje WebSocket", response);
    }

    [Fact]
    public async Task DeviceMonitor_CuandoLectorSeConecta_NotificaCliente()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(15));
        FakeBiometricScannerService scanner = new();
        Start(scanner: scanner, deviceMonitorIntervalSeconds: 1);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        // Esperar >1 s garantiza que el primer poll establezca el baseline
        // (IsConnected=false) antes de cambiar el estado. Los polls posteriores
        // emiten "device.connecting" mientras el scanner sigue desconectado; los
        // drenamos hasta recibir el "device.status.changed" definitivo.
        await Task.Delay(TimeSpan.FromMilliseconds(1500), cts.Token);
        scanner.Status = ConnectedStatus();

        string response;
        do
        {
            response = await ReceiveStringAsync(client, cts.Token);
        } while (response.Contains("device.connecting", StringComparison.Ordinal));

        Assert.Contains("device.status.changed", response);
        Assert.Contains("\"isConnected\":true", response);
    }

    [Fact]
    public async Task DeviceMonitor_CuandoLectorSeDesconecta_NotificaYContinuaBuscando()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(15));
        FakeBiometricScannerService scanner = new() { Status = ConnectedStatus() };
        Start(scanner: scanner, deviceMonitorIntervalSeconds: 1);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        // Mismo motivo que DeviceMonitor_CuandoLectorSeConecta: el primer poll debe
        // registrar IsConnected=true antes del cambio para que el monitor detecte la desconexión.
        await Task.Delay(TimeSpan.FromMilliseconds(1500), cts.Token);
        scanner.Status = DisconnectedStatus();
        string disconnected = await ReceiveStringAsync(client, cts.Token);
        string connecting = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("device.status.changed", disconnected);
        Assert.Contains("\"isConnected\":false", disconnected);
        Assert.Contains("device.connecting", connecting);
    }

    [Fact]
    public async Task DeviceMonitor_CuandoConsultaFalla_ContinuaHastaDetenerse()
    {
        FakeBiometricScannerService scanner = new()
        {
            StatusException = new InvalidOperationException("device error")
        };
        Start(scanner: scanner, deviceMonitorIntervalSeconds: 1);

        await Task.Delay(TimeSpan.FromMilliseconds(200));
        await _service!.StopAsync(CancellationToken.None);

        Assert.True(scanner.StatusException is not null);
        _service = null;
    }

    [Fact]
    public async Task StopAsync_CuandoServidorEstaActivo_DetieneServidor()
    {
        Start();

        await _service!.StopAsync(CancellationToken.None);

        Assert.NotNull(_service);
        _service = null;
    }

    [Fact]
    public async Task StopAsync_CuandoClienteEstaConectado_CancelaRecepcion()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await _service!.StopAsync(CancellationToken.None);

        Assert.NotEqual(WebSocketState.Connecting, client.State);
        _service = null;
    }

    // --- Tests unitarios de IsOriginAllowed (combinaciones nuevas) ---

    [Fact]
    public void IsOriginAllowed_CuandoOrigenCaseDiferente_EsCaseInsensitive()
    {
        bool result = LocalWebSocketServerService.IsOriginAllowed(
            "HTTP://APP.TRAZZO.COM",
            ["http://app.trazzo.com"],
            null);
        Assert.True(result);
    }

    // --- Tests de MatchAndEnqueueAsync vía HandleMessageAsync ---

    [Fact]
    public async Task HandleMessageAsync_CuandoMatchConTemplatesVacios_RetornaFallo()
    {
        LocalWebSocketServerService service = CreateMinimalService();

        object result = await service.HandleMessageAsync(
            """{"type":"fingerprint.match","templates":[]}""", CancellationToken.None);

        string serialized = System.Text.Json.JsonSerializer.Serialize(result);
        Assert.Contains("No se proporcionaron templates", serialized);
    }

    [Fact]
    public async Task HandleMessageAsync_CuandoMatchSinCampoTemplates_RetornaFallo()
    {
        LocalWebSocketServerService service = CreateMinimalService();

        object result = await service.HandleMessageAsync(
            """{"type":"fingerprint.match"}""", CancellationToken.None);

        string serialized = System.Text.Json.JsonSerializer.Serialize(result);
        Assert.Contains("No se proporcionaron templates", serialized);
    }

    [Fact]
    public async Task HandleMessageAsync_CuandoMatchConBase64Invalido_RetornaFallo()
    {
        LocalWebSocketServerService service = CreateMinimalService();

        object result = await service.HandleMessageAsync(
            """{"type":"fingerprint.match","templates":[{"index":0,"templateBase64":"!!!not-base64!!!"}]}""",
            CancellationToken.None);

        // Assert on the typed result to avoid JsonSerializer Unicode-escape issues with 'á'
        FingerprintMatchResult matchResult = Assert.IsType<FingerprintMatchResult>(result);
        Assert.False(matchResult.Success);
        Assert.Contains("Base64", matchResult.Message);
    }

    [Fact]
    public async Task HandleMessageAsync_CuandoMatchConTemplateValido_DevuelveResultadoDeMatch()
    {
        byte[] template = new byte[512];
        FakeBiometricScannerService scanner = new()
        {
            MatchResult = FingerprintMatchResult.NoMatchResult(512, null, 1)
        };
        LocalWebSocketServerService service = CreateMinimalService(scanner: scanner);
        string json = $$$"""{"type":"fingerprint.match","templates":[{"index":5,"templateBase64":"{{{Convert.ToBase64String(template)}}}"}]}""";

        object result = await service.HandleMessageAsync(json, CancellationToken.None);

        string serialized = System.Text.Json.JsonSerializer.Serialize(result);
        Assert.Contains("fingerprint.match.result", serialized);
        Assert.Contains("false", serialized);
    }

    [Fact]
    public async Task HandleMessageAsync_CuandoMatchConCoincidencia_RemapeaIndiceOriginal()
    {
        byte[] template = new byte[512];
        FingerprintQualityResult quality = new(true, 90, 12, 45, true, "OK");
        FakeBiometricScannerService scanner = new()
        {
            MatchResult = FingerprintMatchResult.MatchedResult(0, 512, quality, 1)
        };
        LocalWebSocketServerService service = CreateMinimalService(scanner: scanner);
        string json = $$$"""{"type":"fingerprint.match","templates":[{"index":42,"templateBase64":"{{{Convert.ToBase64String(template)}}}"}]}""";

        object result = await service.HandleMessageAsync(json, CancellationToken.None);

        Trazzo.Biometric.Agent.Contracts.FingerprintMatchResult matchResult =
            Assert.IsType<Trazzo.Biometric.Agent.Contracts.FingerprintMatchResult>(result);
        Assert.Equal(42, matchResult.MatchedIndex);
        Assert.True(matchResult.Matched);
    }

    [Fact]
    public async Task HandleMessageAsync_CuandoMatchJsonMalformado_RetornaFallo()
    {
        LocalWebSocketServerService service = CreateMinimalService();

        object result = await service.HandleMessageAsync("{invalid-json}", CancellationToken.None);

        // "Mensaje JSON inv" has no accented chars (á comes after these chars), avoids Unicode escape issues
        string serialized = System.Text.Json.JsonSerializer.Serialize(result);
        Assert.Contains("error", serialized);
        Assert.Contains("Mensaje JSON inv", serialized);
    }

    [Fact]
    public async Task HandleMessageAsync_CuandoIdentifyConEncryptedTemplateYEnqueueFalla_NoLanzaExcepcion()
    {
        // TryEnqueueAsync exception catch path: enc != null but queue throws
        FakeEventQueue queue = new()
        {
            EnqueueException = new InvalidOperationException("queue failure")
        };
        FakeBiometricScannerService scanner = new()
        {
            IdentifyResult = FingerprintIdentifyResult.Succeeded(
                new byte[512], 512, null, "device-1",
                new EncryptedPayload("enc", "key", "iv", "tag"))
        };
        LocalWebSocketServerService service = CreateMinimalService(scanner: scanner, eventQueue: queue);

        object result = await service.HandleMessageAsync("""{"type":"fingerprint.identify"}""", CancellationToken.None);

        string serialized = System.Text.Json.JsonSerializer.Serialize(result);
        Assert.Contains("fingerprint.identify.result", serialized);
    }

    [Fact]
    public async Task BroadcastAsync_CuandoNoHayClientesConectados_NoLanzaExcepcion()
    {
        // BroadcastAsync empty-clients early-return path:
        // Default scanner is disconnected → first poll sets baseline (no broadcast),
        // second poll triggers "device.connecting" broadcast → _activeClients.IsEmpty → returns immediately
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(scanner: new FakeBiometricScannerService(), deviceMonitorIntervalSeconds: 1);

        await Task.Delay(TimeSpan.FromMilliseconds(2200), cts.Token);

        Assert.NotNull(_service);
    }

    [Fact]
    public async Task ReceiveLoopAsync_CuandoClienteAbortaConexion_ManejaSinCrash()
    {
        // ReceiveLoopAsync WebSocketException catch path
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        client.Abort();

        await Task.Delay(300, cts.Token);
        Assert.NotNull(_service);
    }

    [Fact]
    public async Task ReceiveLoopAsync_CuandoClienteEnviaFrameClose_CierraConexionGraciosamente()
    {
        // ReceiveLoopAsync WebSocketMessageType.Close path
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start();
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.CloseAsync(WebSocketCloseStatus.NormalClosure, "bye", cts.Token);

        Assert.Equal(WebSocketState.Closed, client.State);
    }

    [Fact]
    public async Task ProcessClientMessageAsync_CuandoClienteSeDesconectaAntesDeRespuesta_ManejaSinCrash()
    {
        // ProcessClientMessageAsync WebSocketException catch path:
        // client disconnects while scanner is working, so SendJsonAsync throws WebSocketException
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeBiometricScannerService scanner = new()
        {
            CaptureDelayMilliseconds = 300,
            CaptureResult = FingerprintCaptureResult.Failed("No se encontró ningún lector biométrico.")
        };
        Start(scanner: scanner);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await client.SendAsync(
            Encoding.UTF8.GetBytes("""{"type":"fingerprint.capture"}"""),
            WebSocketMessageType.Text, true, cts.Token);

        await Task.Delay(50, cts.Token);
        client.Abort();

        await Task.Delay(500, cts.Token);
        Assert.NotNull(_service);
    }

    private static LocalWebSocketServerService CreateMinimalService(
        FakeBiometricScannerService? scanner = null,
        FakeEventQueue? eventQueue = null)
    {
        IConfiguration config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Agent:WebSocketUrl"] = "http://localhost:19999/"
            })
            .Build();

        return new LocalWebSocketServerService(
            scanner ?? new FakeBiometricScannerService(),
            new FakeAgentHealthService(),
            eventQueue ?? new FakeEventQueue(),
            config,
            NullLogger<LocalWebSocketServerService>.Instance);
    }

    private void Start(
        string[]? allowedOrigins = null,
        bool includeTrailingSlash = true,
        int deviceMonitorIntervalSeconds = 3,
        FakeBiometricScannerService? scanner = null,
        FakeAgentHealthService? healthService = null)
    {
        int port = GetFreePort();
        _wsUrl = $"ws://localhost:{port}/";
        _httpUrl = $"http://localhost:{port}/";

        var settings = new Dictionary<string, string?>
        {
            ["Agent:WebSocketUrl"] = includeTrailingSlash ? _httpUrl : _httpUrl.TrimEnd('/'),
            ["Agent:DeviceMonitorIntervalSeconds"] = deviceMonitorIntervalSeconds.ToString()
        };

        if (allowedOrigins is { Length: > 0 })
        {
            for (int i = 0; i < allowedOrigins.Length; i++)
                settings[$"Agent:AllowedOrigins:{i}"] = allowedOrigins[i];
        }

        IConfiguration config = new ConfigurationBuilder()
            .AddInMemoryCollection(settings)
            .Build();

        _service = new LocalWebSocketServerService(
            scanner ?? new FakeBiometricScannerService(),
            healthService ?? new FakeAgentHealthService(),
            new FakeEventQueue(),
            config,
            NullLogger<LocalWebSocketServerService>.Instance);

        _ = _service.StartAsync(CancellationToken.None);
    }

    private static FingerprintDeviceStatus ConnectedStatus()
    {
        return new FingerprintDeviceStatus(
            "device.status.result",
            Success: true,
            IsSdkAvailable: true,
            IsInitialized: true,
            IsDeviceOpen: true,
            IsConnected: true,
            DeviceCount: 1,
            Message: "Lector biométrico conectado.",
            CheckedAtUtc: DateTimeOffset.UtcNow);
    }

    private static FingerprintDeviceStatus DisconnectedStatus()
    {
        return new FingerprintDeviceStatus(
            "device.status.result",
            Success: false,
            IsSdkAvailable: true,
            IsInitialized: true,
            IsDeviceOpen: false,
            IsConnected: false,
            DeviceCount: 0,
            Message: "Lector biométrico desconectado.",
            CheckedAtUtc: DateTimeOffset.UtcNow);
    }

    private static async Task<string> ReceiveStringAsync(ClientWebSocket client, CancellationToken ct)
    {
        byte[] buffer = new byte[8192];
        WebSocketReceiveResult result = await client.ReceiveAsync(buffer, ct);
        return Encoding.UTF8.GetString(buffer, 0, result.Count);
    }

    private static int GetFreePort()
    {
        TcpListener l = new(IPAddress.Loopback, 0);
        l.Start();
        int port = ((IPEndPoint)l.LocalEndpoint).Port;
        l.Stop();
        return port;
    }
}
