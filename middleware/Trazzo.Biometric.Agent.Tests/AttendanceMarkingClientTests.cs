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
        // Bytes reales para poder validar el empaquetado iv||cipher||tag.
        byte[] iv = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
        byte[] cipher = [100, 101, 102];
        byte[] tag = [20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35];
        EncryptedPayload encrypted = new(
            Convert.ToBase64String(cipher),
            "aeskey",
            Convert.ToBase64String(iv),
            Convert.ToBase64String(tag));
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
        // Contrato del backend (BiometricIdentifyRequest): iv y tag SEPARADOS.
        Assert.Equal("aeskey", root.GetProperty("encrypted_aes_key_base64").GetString());
        Assert.Equal("ZK9500-CONFIG", root.GetProperty("device_code").GetString());
        // captured_at_utc: LocalDateTime sin offset (UTC, milisegundos).
        Assert.Equal("2026-07-01T12:30:00.000", root.GetProperty("captured_at_utc").GetString());

        Assert.Equal(Convert.ToBase64String(cipher), root.GetProperty("encrypted_template_base64").GetString());
        Assert.Equal(Convert.ToBase64String(iv), root.GetProperty("iv_base64").GetString());
        Assert.Equal(Convert.ToBase64String(tag), root.GetProperty("tag_base64").GetString());

        // Ya no se envía el formato empaquetado anterior.
        Assert.False(root.TryGetProperty("template_cifrado", out _));
        Assert.False(root.TryGetProperty("llave_cifrado", out _));
        Assert.False(root.TryGetProperty("capturado_en", out _));
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
            ValidEncryptedPayload(),
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
            ValidEncryptedPayload(),
            deviceId: "ZK9500-CAPTURED",
            DateTimeOffset.UtcNow,
            CancellationToken.None);

        Assert.False(sent);
        Assert.NotNull(handler.LastRequest);
    }

    [Fact]
    public async Task TryMarkAsync_WhenDisabled_ReturnsFalseWithoutSending()
    {
        MockHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AttendanceMarkingClient client = CreateClient(httpClient, new Dictionary<string, string?>
        {
            // No Backend:BaseUrl → _attendanceMarkUrl is null → _isEnabled = false
        });

        bool sent = await client.TryMarkAsync(
            ValidEncryptedPayload(),
            deviceId: "ZK9500-1",
            DateTimeOffset.UtcNow,
            CancellationToken.None);

        Assert.False(sent);
        Assert.Null(handler.LastRequest);
    }

    [Fact]
    public async Task TryMarkAsync_WhenHttpThrows_ReturnsFalse()
    {
        ThrowingHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AttendanceMarkingClient client = CreateClient(httpClient);

        bool sent = await client.TryMarkAsync(
            ValidEncryptedPayload(),
            deviceId: "ZK9500-CAPTURED",
            DateTimeOffset.UtcNow,
            CancellationToken.None);

        Assert.False(sent);
    }

    // Payload con IV/tag/cipher válidos en base64 (para pruebas donde el contenido
    // no importa pero ToPackedTemplateBase64 debe poder decodificar).
    private static EncryptedPayload ValidEncryptedPayload() => new(
        EncryptedTemplateBase64: Convert.ToBase64String([1, 2, 3]),
        EncryptedAesKeyBase64: Convert.ToBase64String([4, 5, 6]),
        IvBase64: Convert.ToBase64String(new byte[12]),
        TagBase64: Convert.ToBase64String(new byte[16]));

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
