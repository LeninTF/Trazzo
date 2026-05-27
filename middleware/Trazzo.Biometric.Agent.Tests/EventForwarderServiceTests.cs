using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Queue;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class EventForwarderServiceTests
{
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

    private static EventForwarderService CreateForwarder(
        FakeEventQueue queue,
        Func<BiometricEvent, CancellationToken, Task<bool>>? sender = null)
    {
        return new EventForwarderService(
            queue,
            sender ?? ((_, _) => Task.FromResult(true)),
            retryIntervalSeconds: 60,
            NullLogger<EventForwarderService>.Instance);
    }

    private static BiometricEvent MakeEvent(long id) => new()
    {
        Id = id,
        EventType = "capture",
        EncryptedTemplateBase64 = "abc",
        EncryptedAesKeyBase64 = "def",
        IvBase64 = "ghi",
        TagBase64 = "jkl",
        DeviceId = "ZK9500-1",
        CapturedAtUtc = DateTimeOffset.UtcNow
    };
}
