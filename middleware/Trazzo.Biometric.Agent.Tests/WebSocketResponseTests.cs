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

    private static LocalWebSocketServerService CreateServer(
        int deviceCount = 0,
        FingerprintCaptureResult? captureResult = null)
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
            new ConfigurationBuilder().Build(),
            NullLogger<LocalWebSocketServerService>.Instance);
    }

    private static JsonDocument ToJsonDocument(object response)
    {
        byte[] json = JsonSerializer.SerializeToUtf8Bytes(response, JsonOptions);
        return JsonDocument.Parse(json);
    }
}
