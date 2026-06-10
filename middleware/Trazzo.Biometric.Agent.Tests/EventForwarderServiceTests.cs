using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Queue;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class EventForwarderServiceTests
{
    [Fact]
    public async Task Constructor_CuandoBackendNoConfigurado_DeshabilitaEjecucion()
    {
        IConfiguration configuration = new ConfigurationBuilder().Build();
        EventForwarderService forwarder = new(
            new FakeEventQueue(),
            configuration,
            NullLogger<EventForwarderService>.Instance);

        await forwarder.StartAsync(CancellationToken.None);
        await forwarder.StopAsync(CancellationToken.None);
    }

    [Fact]
    public void Constructor_CuandoBackendConfigurado_CreaServicioHabilitado()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:BackendUrl"] = "https://localhost/api/asistencias/sync",
                ["Queue:AgentToken"] = "token",
                ["Agent:TenantId"] = "tenant-1",
                ["Queue:RetryIntervalSeconds"] = "1"
            })
            .Build();

        EventForwarderService forwarder = new(
            new FakeEventQueue(),
            configuration,
            NullLogger<EventForwarderService>.Instance);

        Assert.NotNull(forwarder);
    }

    [Fact]
    public async Task ExecuteAsync_CuandoSeDetiene_CancelaEspera()
    {
        TaskCompletionSource senderCalled = new(TaskCreationOptions.RunContinuationsAsynchronously);
        EventForwarderService forwarder = CreateForwarder(
            new FakeEventQueue { PendingToReturn = [MakeEvent(id: 1)] },
            sender: (_, _) =>
            {
                senderCalled.TrySetResult();
                return Task.FromResult(false);
            },
            retryIntervalSeconds: 1);

        await forwarder.StartAsync(CancellationToken.None);
        await senderCalled.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await forwarder.StopAsync(CancellationToken.None);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoColaVacia_NoMarcaNadaComoEnviado()
    {
        FakeEventQueue queue = new() { PendingToReturn = [] };
        EventForwarderService forwarder = CreateForwarder(queue);

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Empty(queue.SentIds);
        Assert.Empty(queue.FailedIds);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoSenderExitoso_MarcaEventosComoEnviados()
    {
        BiometricEvent evt1 = MakeEvent(id: 1);
        BiometricEvent evt2 = MakeEvent(id: 2);
        FakeEventQueue queue = new() { PendingToReturn = [evt1, evt2] };
        EventForwarderService forwarder = CreateForwarder(queue, sender: (_, _) => Task.FromResult(true));

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Single(queue.SentIds);
        Assert.Contains(1L, queue.SentIds[0]);
        Assert.Contains(2L, queue.SentIds[0]);
        Assert.Empty(queue.FailedIds);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoSenderFalla_MarcaEventoComoFallido()
    {
        BiometricEvent evt = MakeEvent(id: 7);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = CreateForwarder(queue, sender: (_, _) => Task.FromResult(false));

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Empty(queue.SentIds);
        Assert.Contains(7L, queue.FailedIds);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoSenderLanzaExcepcion_MarcaEventoComoFallido()
    {
        BiometricEvent evt = MakeEvent(id: 3);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = CreateForwarder(queue,
            sender: (_, _) => Task.FromException<bool>(new HttpRequestException("timeout")));

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Empty(queue.SentIds);
        Assert.Contains(3L, queue.FailedIds);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoAlgunosExitososYOtrosFallidos_MarcaCorrectamente()
    {
        BiometricEvent evt1 = MakeEvent(id: 10);
        BiometricEvent evt2 = MakeEvent(id: 20);
        BiometricEvent evt3 = MakeEvent(id: 30);
        FakeEventQueue queue = new() { PendingToReturn = [evt1, evt2, evt3] };

        int callCount = 0;
        EventForwarderService forwarder = CreateForwarder(queue, sender: (_, _) =>
        {
            callCount++;
            return Task.FromResult(callCount != 2); // segundo falla
        });

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Single(queue.SentIds);
        Assert.Contains(10L, queue.SentIds[0]);
        Assert.Contains(30L, queue.SentIds[0]);
        Assert.Contains(20L, queue.FailedIds);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoColaVacia_RetornaFalse()
    {
        FakeEventQueue queue = new() { PendingToReturn = [] };
        EventForwarderService forwarder = CreateForwarder(queue);

        bool hadFailures = await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.False(hadFailures);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoTodosEnviados_RetornaFalse()
    {
        BiometricEvent evt = MakeEvent(id: 1);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = CreateForwarder(queue, sender: (_, _) => Task.FromResult(true));

        bool hadFailures = await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.False(hadFailures);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoSenderFalla_RetornaTrue()
    {
        BiometricEvent evt = MakeEvent(id: 1);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = CreateForwarder(queue, sender: (_, _) => Task.FromResult(false));

        bool hadFailures = await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.True(hadFailures);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoSenderLanzaExcepcion_RetornaTrue()
    {
        BiometricEvent evt = MakeEvent(id: 5);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = CreateForwarder(queue,
            sender: (_, _) => Task.FromException<bool>(new HttpRequestException("timeout")));

        bool hadFailures = await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.True(hadFailures);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoColaLanzaExcepcion_RetornaTrue()
    {
        EventForwarderService forwarder = new(
            new ThrowingEventQueue(new InvalidOperationException("database unavailable")),
            (_, _) => Task.FromResult(true),
            retryIntervalSeconds: 60,
            NullLogger<EventForwarderService>.Instance);

        bool hadFailures = await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.True(hadFailures);
    }

    [Fact]
    public async Task TryForwardPendingAsync_CuandoColaCancela_PropagaCancelacion()
    {
        EventForwarderService forwarder = new(
            new ThrowingEventQueue(new OperationCanceledException()),
            (_, _) => Task.FromResult(true),
            retryIntervalSeconds: 60,
            NullLogger<EventForwarderService>.Instance);

        await Assert.ThrowsAsync<OperationCanceledException>(
            () => forwarder.TryForwardPendingAsync(CancellationToken.None));
    }

    [Fact]
    public async Task BuildHttpSender_CuandoEnviaEvento_PayloadTieneFormatoCorrecto()
    {
        byte[] iv = new byte[12];
        byte[] tag = new byte[16];
        byte[] ciphertext = [1, 2, 3];
        string llaveCifrada = Convert.ToBase64String([4, 5, 6]);
        DateTimeOffset capturedAt = new(2026, 5, 27, 10, 0, 0, TimeSpan.Zero);

        BiometricEvent evt = new()
        {
            Id = 1,
            EventType = "capture",
            EncryptedTemplateBase64 = Convert.ToBase64String(ciphertext),
            EncryptedAesKeyBase64 = llaveCifrada,
            IvBase64 = Convert.ToBase64String(iv),
            TagBase64 = Convert.ToBase64String(tag),
            DeviceId = "ZK9500-12345",
            CapturedAtUtc = capturedAt
        };

        MockHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = new(
            queue, httpClient, "http://localhost/api/asistencias/sync", null, 60,
            NullLogger<EventForwarderService>.Instance);

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.NotNull(handler.LastRequestBody);
        using JsonDocument doc = JsonDocument.Parse(handler.LastRequestBody!);
        JsonElement root = doc.RootElement;

        // templateCifrado = base64(IV[12] + Tag[16] + Ciphertext)
        byte[] combined = [.. iv, .. tag, .. ciphertext];
        Assert.Equal(Convert.ToBase64String(combined), root.GetProperty("templateCifrado").GetString());
        Assert.Equal(llaveCifrada, root.GetProperty("llaveCifrada").GetString());
        Assert.Equal("ZK9500-12345", root.GetProperty("dispositivoId").GetString());
        Assert.Equal(capturedAt.ToString("O"), root.GetProperty("timestampLocal").GetString());
    }

    [Fact]
    public async Task BuildHttpSender_CuandoTokenConfigurado_EnviaAuthorizationHeader()
    {
        BiometricEvent evt = MakeEvent(id: 1);
        MockHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = new(
            queue, httpClient, "http://localhost/api/asistencias/sync", "mi-token-jwt", 60,
            NullLogger<EventForwarderService>.Instance);

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Equal("Bearer", handler.LastRequest?.Headers.Authorization?.Scheme);
        Assert.Equal("mi-token-jwt", handler.LastRequest?.Headers.Authorization?.Parameter);
    }

    [Fact]
    public async Task BuildHttpSender_CuandoSinToken_NoEnviaAuthorizationHeader()
    {
        BiometricEvent evt = MakeEvent(id: 1);
        MockHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        FakeEventQueue queue = new() { PendingToReturn = [evt] };
        EventForwarderService forwarder = new(
            queue, httpClient, "http://localhost/api/asistencias/sync", null, 60,
            NullLogger<EventForwarderService>.Instance);

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Null(handler.LastRequest?.Headers.Authorization);
    }

    [Fact]
    public async Task BuildHttpSender_CuandoTenantConfigurado_EnviaTenantHeader()
    {
        MockHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        EventForwarderService forwarder = new(
            new FakeEventQueue { PendingToReturn = [MakeEvent(id: 1)] },
            httpClient,
            "http://localhost/api/asistencias/sync",
            agentToken: null,
            retryIntervalSeconds: 60,
            NullLogger<EventForwarderService>.Instance,
            tenantId: "tenant-1");

        await forwarder.TryForwardPendingAsync(CancellationToken.None);

        Assert.Equal("tenant-1", handler.LastRequest?.Headers.GetValues("X-Tenant-ID").Single());
    }

    private static EventForwarderService CreateForwarder(
        FakeEventQueue queue,
        Func<BiometricEvent, CancellationToken, Task<bool>>? sender = null,
        int retryIntervalSeconds = 60)
    {
        return new EventForwarderService(
            queue,
            sender ?? ((_, _) => Task.FromResult(true)),
            retryIntervalSeconds,
            NullLogger<EventForwarderService>.Instance);
    }

    private static BiometricEvent MakeEvent(long id) => new()
    {
        Id = id,
        EventType = "capture",
        EncryptedTemplateBase64 = Convert.ToBase64String([1, 2, 3]),
        EncryptedAesKeyBase64 = Convert.ToBase64String([4, 5, 6]),
        IvBase64 = Convert.ToBase64String(new byte[12]),
        TagBase64 = Convert.ToBase64String(new byte[16]),
        DeviceId = "ZK9500-1",
        CapturedAtUtc = DateTimeOffset.UtcNow
    };

    private sealed class ThrowingEventQueue(Exception exception) : IEventQueue
    {
        public Task EnqueueAsync(BiometricEvent biometricEvent, CancellationToken cancellationToken = default)
            => Task.FromException(exception);

        public Task<IReadOnlyList<BiometricEvent>> GetPendingAsync(
            int limit = 50,
            CancellationToken cancellationToken = default)
            => Task.FromException<IReadOnlyList<BiometricEvent>>(exception);

        public Task MarkSentAsync(IEnumerable<long> ids, CancellationToken cancellationToken = default)
            => Task.FromException(exception);

        public Task MarkFailedAsync(long id, CancellationToken cancellationToken = default)
            => Task.FromException(exception);

        public Task<int> GetPendingCountAsync(CancellationToken cancellationToken = default)
            => Task.FromException<int>(exception);

        public Task PruneAsync(TimeSpan maxAge, CancellationToken cancellationToken = default)
            => Task.FromException(exception);
    }
}
