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
    public async Task OriginAuth_CuandoListaVacia_AceptaCualquierOrigen()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(allowedOrigins: []);

        using ClientWebSocket client = new();
        client.Options.SetRequestHeader("Origin", "http://cualquier-dominio.com");
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
    public async Task WebSocket_CuandoRepiteOperacionBiometrica_AplicaRateLimit()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        Start(rateLimitSeconds: 30);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);
        byte[] request = Encoding.UTF8.GetBytes("""{"type":"fingerprint.capture"}""");

        await client.SendAsync(request, WebSocketMessageType.Text, true, cts.Token);
        await ReceiveStringAsync(client, cts.Token);
        await client.SendAsync(request, WebSocketMessageType.Text, true, cts.Token);
        string response = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("Demasiadas solicitudes", response);
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
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeBiometricScannerService scanner = new();
        Start(scanner: scanner, deviceMonitorIntervalSeconds: 1);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await Task.Delay(TimeSpan.FromMilliseconds(200), cts.Token);
        scanner.Status = ConnectedStatus();
        string response = await ReceiveStringAsync(client, cts.Token);

        Assert.Contains("device.status.changed", response);
        Assert.Contains("\"isConnected\":true", response);
    }

    [Fact]
    public async Task DeviceMonitor_CuandoLectorSeDesconecta_NotificaYContinuaBuscando()
    {
        using var cts = new CancellationTokenSource(TimeSpan.FromSeconds(10));
        FakeBiometricScannerService scanner = new() { Status = ConnectedStatus() };
        Start(scanner: scanner, deviceMonitorIntervalSeconds: 1);
        using ClientWebSocket client = new();
        await client.ConnectAsync(new Uri(_wsUrl), cts.Token);

        await Task.Delay(TimeSpan.FromMilliseconds(200), cts.Token);
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

        _service = null;
    }

    [Fact]
    public async Task StopAsync_CuandoServidorEstaActivo_DetieneServidor()
    {
        Start();

        await _service!.StopAsync(CancellationToken.None);

        _service = null;
    }

    private void Start(
        string[]? allowedOrigins = null,
        bool includeTrailingSlash = true,
        int rateLimitSeconds = 5,
        int deviceMonitorIntervalSeconds = 3,
        FakeBiometricScannerService? scanner = null)
    {
        int port = GetFreePort();
        _wsUrl = $"ws://localhost:{port}/";
        _httpUrl = $"http://localhost:{port}/";

        var settings = new Dictionary<string, string?>
        {
            ["Agent:WebSocketUrl"] = includeTrailingSlash ? _httpUrl : _httpUrl.TrimEnd('/'),
            ["Agent:RateLimitSeconds"] = rateLimitSeconds.ToString(),
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
            new FakeAgentHealthService(),
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
