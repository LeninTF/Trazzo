using System.Text.Json;
using System.Net;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Queue;
using Trazzo.Biometric.Agent.WebSocket;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class WebSocketResponseTests
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    [Fact]
    public async Task HandleMessageAsync_ForHealthCheck_ReturnsHealthResultJson()
    {
        LocalWebSocketServerService server = CreateServer();

        object response = await server.HandleMessageAsync("""{ "type": "health.check" }""", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("health.check.result", root.GetProperty("type").GetString());
        Assert.True(root.GetProperty("success").GetBoolean());
        Assert.Equal("El agente biométrico de Trazzo está en ejecución.", root.GetProperty("message").GetString());
    }

    [Fact]
    public async Task HandleMessageAsync_ForDeviceStatus_ReturnsDeviceStatusJson()
    {
        LocalWebSocketServerService server = CreateServer(deviceCount: 1);

        object response = await server.HandleMessageAsync("""{ "type": "device.status" }""", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("device.status.result", root.GetProperty("type").GetString());
        Assert.True(root.GetProperty("success").GetBoolean());
        Assert.Equal("Lector biométrico encontrado.", root.GetProperty("message").GetString());
        Assert.Equal(1, root.GetProperty("deviceCount").GetInt32());
    }

    [Fact]
    public async Task HandleMessageAsync_ForFingerprintCapture_ReturnsCaptureResultJson()
    {
        FingerprintCaptureResult captureResult = FingerprintCaptureResult.Succeeded([9, 8, 7], 3);
        LocalWebSocketServerService server = CreateServer(captureResult: captureResult);

        object response = await server.HandleMessageAsync("""{ "type": "fingerprint.capture" }""", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("fingerprint.capture.result", root.GetProperty("type").GetString());
        Assert.True(root.GetProperty("success").GetBoolean());
        Assert.Equal(Convert.ToBase64String([9, 8, 7]), root.GetProperty("templateBase64").GetString());
        Assert.Equal(3, root.GetProperty("templateSize").GetInt32());
    }

    [Fact]
    public async Task HandleMessageAsync_ForInvalidJson_ReturnsErrorJson()
    {
        LocalWebSocketServerService server = CreateServer();

        object response = await server.HandleMessageAsync("{ invalid json", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("error", root.GetProperty("type").GetString());
        Assert.False(root.GetProperty("success").GetBoolean());
        Assert.Equal("Mensaje JSON inválido.", root.GetProperty("message").GetString());
    }

    [Fact]
    public async Task HandleMessageAsync_ForQueueStatus_ReturnsPendingCount()
    {
        FakeEventQueue queue = new() { PendingCount = 3 };
        LocalWebSocketServerService server = CreateServer(eventQueue: queue);

        object response = await server.HandleMessageAsync("""{ "type": "queue.status" }""", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("queue.status.result", root.GetProperty("type").GetString());
        Assert.True(root.GetProperty("success").GetBoolean());
        Assert.Equal(3, root.GetProperty("pendingCount").GetInt32());
    }

    [Fact]
    public async Task HandleMessageAsync_ForCaptureWithEncryptedTemplate_EnqueuesEvent()
    {
        EncryptedPayload enc = new("cipher", "key", "iv", "tag");
        FingerprintCaptureResult captureResult = FingerprintCaptureResult.Succeeded(
            [1, 2, 3],
            3,
            new FingerprintCaptureOptions(DeviceId: "ZK9500-1", EncryptedTemplate: enc));
        FakeEventQueue queue = new();
        LocalWebSocketServerService server = CreateServer(captureResult: captureResult, eventQueue: queue);

        await server.HandleMessageAsync("""{ "type": "fingerprint.capture" }""", CancellationToken.None);

        Assert.Single(queue.Enqueued);
        Assert.Equal("capture", queue.Enqueued[0].EventType);
        Assert.Equal("ZK9500-1", queue.Enqueued[0].DeviceId);
    }

    [Fact]
    public async Task HandleMessageAsync_ForCaptureWithoutEncryptedTemplate_DoesNotEnqueue()
    {
        FingerprintCaptureResult captureResult = FingerprintCaptureResult.Succeeded([1, 2, 3], 3);
        FakeEventQueue queue = new();
        LocalWebSocketServerService server = CreateServer(captureResult: captureResult, eventQueue: queue);

        await server.HandleMessageAsync("""{ "type": "fingerprint.capture" }""", CancellationToken.None);

        Assert.Empty(queue.Enqueued);
    }

    [Fact]
    public async Task HandleMessageAsync_ForUnsupportedType_ReturnsErrorJson()
    {
        LocalWebSocketServerService server = CreateServer();

        object response = await server.HandleMessageAsync("""{ "type": "tipo.inexistente" }""", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("error", root.GetProperty("type").GetString());
        Assert.False(root.GetProperty("success").GetBoolean());
        Assert.Equal("Tipo de mensaje WebSocket no soportado.", root.GetProperty("message").GetString());
    }

    [Fact]
    public async Task HandleMessageAsync_ForEnrollCancel_RetornaTipoEnrollResult()
    {
        LocalWebSocketServerService server = CreateServer();

        object response = await server.HandleMessageAsync("""{ "type": "fingerprint.enroll.cancel" }""", CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        JsonElement root = document.RootElement;
        Assert.Equal("fingerprint.enroll.result", root.GetProperty("type").GetString());
        Assert.False(root.GetProperty("success").GetBoolean());
    }

    [Fact]
    public async Task HandleMessageAsync_ForIdentifyWithEncryptedTemplate_EnqueuesEvent()
    {
        EncryptedPayload encrypted = new("cipher", "key", "iv", "tag");
        FingerprintIdentifyResult identifyResult = FingerprintIdentifyResult.Succeeded(
            [1, 2, 3],
            3,
            quality: null,
            deviceId: "ZK9500-IDENTIFY",
            encryptedTemplate: encrypted);
        FakeEventQueue queue = new();
        LocalWebSocketServerService server = CreateServer(
            identifyResult: identifyResult,
            eventQueue: queue);

        object response = await server.HandleMessageAsync(
            """{ "type": "fingerprint.identify" }""",
            CancellationToken.None);

        Assert.Equal(identifyResult, response);
        BiometricEvent queued = Assert.Single(queue.Enqueued);
        Assert.Equal("identify", queued.EventType);
        Assert.Equal("ZK9500-IDENTIFY", queued.DeviceId);
    }

    [Fact]
    public async Task HandleMessageAsync_ForEnrollWithEncryptedTemplate_EnqueuesEvent()
    {
        EncryptedPayload encrypted = new("cipher", "key", "iv", "tag");
        FingerprintEnrollResult enrollResult = FingerprintEnrollResult.Succeeded(
            [1, 2, 3],
            3,
            capturedSamples: 3,
            deviceId: "ZK9500-ENROLL",
            encryptedTemplate: encrypted);
        FakeEventQueue queue = new();
        LocalWebSocketServerService server = CreateServer(
            enrollResult: enrollResult,
            eventQueue: queue);

        object response = await server.HandleMessageAsync(
            """{ "type": "fingerprint.enroll.start" }""",
            CancellationToken.None);

        Assert.Equal(enrollResult, response);
        BiometricEvent queued = Assert.Single(queue.Enqueued);
        Assert.Equal("enroll", queued.EventType);
        Assert.Equal("ZK9500-ENROLL", queued.DeviceId);
    }

    [Fact]
    public void TryParseMessageType_ReturnsTypeOrNull()
    {
        Assert.Equal(
            "fingerprint.capture",
            LocalWebSocketServerService.TryParseMessageType("""{"type":"fingerprint.capture"}"""));
        Assert.Null(LocalWebSocketServerService.TryParseMessageType("{ invalid"));
        Assert.Null(LocalWebSocketServerService.TryParseMessageType("null"));
        Assert.Null(LocalWebSocketServerService.TryParseMessageType("{}"));
    }

    [Theory]
    [InlineData(null, "http://localhost:9001/")]
    [InlineData("http://localhost:8080", "http://localhost:8080/")]
    [InlineData("http://localhost:8080/", "http://localhost:8080/")]
    public void ResolveWebSocketUrl_NormalizesConfiguredUrl(string? configuredUrl, string expected)
    {
        Assert.Equal(expected, LocalWebSocketServerService.ResolveWebSocketUrl(configuredUrl));
    }

    [Fact]
    public void ResolveClientId_ReturnsEndpointOrUnknown()
    {
        Assert.Equal("127.0.0.1:9001", LocalWebSocketServerService.ResolveClientId(new IPEndPoint(IPAddress.Loopback, 9001)));
        Assert.Equal("unknown", LocalWebSocketServerService.ResolveClientId(null));
    }

    [Theory]
    [InlineData(null, true)]
    [InlineData("", true)]
    [InlineData("http://localhost:4200", true)]
    [InlineData("http://127.0.0.1:4200", true)]
    [InlineData("http://[::1]:4200", true)]
    [InlineData("http://evil.example", false)]
    [InlineData("null", false)]
    public void IsOriginAllowed_WhenAllowedOriginsEmpty_AllowsOnlyNativeOrLoopback(string? origin, bool expected)
    {
        Assert.Equal(expected, LocalWebSocketServerService.IsOriginAllowed(origin, []));
    }

    [Fact]
    public void IsOriginAllowed_WhenAllowedOriginsConfigured_RequiresExactMatch()
    {
        string[] allowedOrigins = ["https://app.trazzo.pe"];

        Assert.True(LocalWebSocketServerService.IsOriginAllowed("https://app.trazzo.pe", allowedOrigins));
        Assert.False(LocalWebSocketServerService.IsOriginAllowed("https://evil.example", allowedOrigins));
        Assert.False(LocalWebSocketServerService.IsOriginAllowed(null, allowedOrigins));
    }

    [Theory]
    [InlineData("null")]
    [InlineData("{}")]
    public async Task HandleMessageAsync_WhenMessageHasNoType_ReturnsUnsupportedType(string json)
    {
        LocalWebSocketServerService server = CreateServer();

        object response = await server.HandleMessageAsync(json, CancellationToken.None);
        using JsonDocument document = ToJsonDocument(response);

        Assert.Equal("error", document.RootElement.GetProperty("type").GetString());
        Assert.Equal("Tipo de mensaje WebSocket no soportado.", document.RootElement.GetProperty("message").GetString());
    }

    [Fact]
    public void IsRateLimited_WhenEnabled_RejectsSecondOperation()
    {
        LocalWebSocketServerService server = CreateServer(rateLimitSeconds: 5);

        Assert.False(server.IsRateLimited("client-1", "fingerprint.capture"));
        Assert.True(server.IsRateLimited("client-1", "fingerprint.capture"));
    }

    [Fact]
    public void IsRateLimited_WhenDisabled_AllowsRepeatedOperations()
    {
        LocalWebSocketServerService server = CreateServer(rateLimitSeconds: 0);

        Assert.False(server.IsRateLimited("client-1", "fingerprint.capture"));
        Assert.False(server.IsRateLimited("client-1", "fingerprint.capture"));
    }

    private static LocalWebSocketServerService CreateServer(
        int deviceCount = 0,
        FingerprintCaptureResult? captureResult = null,
        FingerprintIdentifyResult? identifyResult = null,
        FingerprintEnrollResult? enrollResult = null,
        FakeEventQueue? eventQueue = null,
        int rateLimitSeconds = 5)
    {
        FakeBiometricScannerService scanner = new()
        {
            Status = new FingerprintDeviceStatus(
                "device.status.result",
                Success: deviceCount > 0,
                IsSdkAvailable: true,
                IsInitialized: true,
                IsDeviceOpen: deviceCount > 0,
                IsConnected: deviceCount > 0,
                DeviceCount: deviceCount,
                Message: deviceCount > 0 ? "Lector biométrico encontrado." : "No se encontró ningún lector biométrico.",
                CheckedAtUtc: DateTimeOffset.UtcNow),
            CaptureResult = captureResult ?? FingerprintCaptureResult.Failed("No se encontró ningún lector biométrico."),
            IdentifyResult = identifyResult ?? FingerprintIdentifyResult.Failed("No se pudo identificar la huella."),
            EnrollResult = enrollResult ?? FingerprintEnrollResult.Failed("No se pudo enrolar la huella.")
        };

        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Agent:RateLimitSeconds"] = rateLimitSeconds.ToString()
            })
            .Build();

        return new LocalWebSocketServerService(
            scanner,
            new FakeAgentHealthService(),
            eventQueue ?? new FakeEventQueue(),
            configuration,
            NullLogger<LocalWebSocketServerService>.Instance);
    }

    private static JsonDocument ToJsonDocument(object response)
    {
        byte[] json = JsonSerializer.SerializeToUtf8Bytes(response, JsonOptions);
        return JsonDocument.Parse(json);
    }
}
