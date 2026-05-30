using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Contracts;
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
        FingerprintCaptureResult captureResult = FingerprintCaptureResult.Succeeded([1, 2, 3], 3, encryptedTemplate: enc, deviceId: "ZK9500-1");
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

    private static LocalWebSocketServerService CreateServer(
        int deviceCount = 0,
        FingerprintCaptureResult? captureResult = null,
        FakeEventQueue? eventQueue = null)
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
            CaptureResult = captureResult ?? FingerprintCaptureResult.Failed("No se encontró ningún lector biométrico.")
        };

        return new LocalWebSocketServerService(
            scanner,
            new FakeAgentHealthService(),
            eventQueue ?? new FakeEventQueue(),
            new ConfigurationBuilder().Build(),
            NullLogger<LocalWebSocketServerService>.Instance);
    }

    private static JsonDocument ToJsonDocument(object response)
    {
        byte[] json = JsonSerializer.SerializeToUtf8Bytes(response, JsonOptions);
        return JsonDocument.Parse(json);
    }
}
