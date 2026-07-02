using System.Net;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Backend;
using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AttendanceMarkingClientTests
{
    [Fact]
    public async Task TryMarkAsync_WhenConfigured_SendsBiometricIdentifyRequest()
    {
        MockHttpMessageHandler handler = new() { ResponseStatusCode = HttpStatusCode.OK };
        using HttpClient httpClient = new(handler);
        AttendanceMarkingClient client = CreateClient(httpClient);
        EncryptedPayload encrypted = new("cipher", "key", "iv", "tag");
        DateTimeOffset capturedAt = new(2026, 7, 1, 12, 30, 0, TimeSpan.Zero);

        bool sent = await client.TryMarkAsync(
            encrypted,
            deviceId: "ZK9500-CAPTURED",
            capturedAt,
            CancellationToken.None);

        Assert.True(sent);
        Assert.Equal(HttpMethod.Post, handler.LastRequest?.Method);
        Assert.Equal("https://api.trazzo.pe/api/v1/asistencia/marcar", handler.LastRequest?.RequestUri?.ToString());
        Assert.Equal("tenant-1", handler.LastRequest?.Headers.GetValues("X-Tenant-ID").Single());
        Assert.Equal("Bearer", handler.LastRequest?.Headers.Authorization?.Scheme);
        Assert.Equal("middleware-token", handler.LastRequest?.Headers.Authorization?.Parameter);

        Assert.NotNull(handler.LastRequestBody);
        using JsonDocument document = JsonDocument.Parse(handler.LastRequestBody!);
        JsonElement root = document.RootElement;
        Assert.Equal(JsonValueKind.Object, root.ValueKind);
        Assert.Equal("identify", root.GetProperty("event_type").GetString());
        Assert.Equal("cipher", root.GetProperty("encrypted_template_base64").GetString());
        Assert.Equal("key", root.GetProperty("encrypted_aes_key_base64").GetString());
        Assert.Equal("iv", root.GetProperty("iv_base64").GetString());
        Assert.Equal("tag", root.GetProperty("tag_base64").GetString());
        Assert.Equal("ZK9500-CONFIG", root.GetProperty("device_code").GetString());
        Assert.Equal(capturedAt.ToString("O"), root.GetProperty("captured_at_utc").GetString());
    }

    [Fact]
    public async Task TryMarkAsync_WhenDeviceCodeMissing_DoesNotSendInvalidPayload()
    {
        MockHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AttendanceMarkingClient client = CreateClient(
            httpClient,
            new Dictionary<string, string?>
            {
                ["Backend:BaseUrl"] = "https://api.trazzo.pe/api/v1",
                ["Agent:TenantId"] = "tenant-1"
            });

        bool sent = await client.TryMarkAsync(
            new EncryptedPayload("cipher", "key", "iv", "tag"),
            deviceId: null,
            DateTimeOffset.UtcNow,
            CancellationToken.None);

        Assert.False(sent);
        Assert.Null(handler.LastRequest);
    }

    [Fact]
    public async Task TryMarkAsync_WhenBackendRejects_ReturnsFalse()
    {
        MockHttpMessageHandler handler = new() { ResponseStatusCode = HttpStatusCode.Forbidden };
        using HttpClient httpClient = new(handler);
        AttendanceMarkingClient client = CreateClient(httpClient);

        bool sent = await client.TryMarkAsync(
            new EncryptedPayload("cipher", "key", "iv", "tag"),
            deviceId: "ZK9500-CAPTURED",
            DateTimeOffset.UtcNow,
            CancellationToken.None);

        Assert.False(sent);
        Assert.NotNull(handler.LastRequest);
    }

    private static AttendanceMarkingClient CreateClient(
        HttpClient httpClient,
        Dictionary<string, string?>? settings = null)
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(settings ?? new Dictionary<string, string?>
            {
                ["Backend:BaseUrl"] = "https://api.trazzo.pe/api/v1",
                ["Backend:Endpoints:AttendanceMark"] = "/asistencia/marcar",
                ["Agent:TenantId"] = "tenant-1",
                ["Agent:DeviceCode"] = "ZK9500-CONFIG",
                ["Queue:AgentToken"] = "middleware-token"
            })
            .Build();

        return new AttendanceMarkingClient(
            configuration,
            NullLogger<AttendanceMarkingClient>.Instance,
            httpClient);
    }
}
