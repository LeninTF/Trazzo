using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
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

    private void Start(string[]? allowedOrigins = null)
    {
        int port = GetFreePort();
        _wsUrl = $"ws://localhost:{port}/";
        _httpUrl = $"http://localhost:{port}/";

        var settings = new Dictionary<string, string?>
        {
            ["Agent:WebSocketUrl"] = _httpUrl
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
            new FakeBiometricScannerService(),
            new FakeAgentHealthService(),
            new FakeEventQueue(),
            config,
            NullLogger<LocalWebSocketServerService>.Instance);

        _ = _service.StartAsync(CancellationToken.None);
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
