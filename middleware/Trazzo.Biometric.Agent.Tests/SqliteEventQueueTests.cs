using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Queue;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class SqliteEventQueueTests
{
    [Fact]
    public async Task EnqueueAsync_IncrementesPendingCount()
    {
        using SqliteEventQueue queue = CreateQueue();

        await queue.EnqueueAsync(MakeEvent("capture"));
        await queue.EnqueueAsync(MakeEvent("identify"));

        Assert.Equal(2, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task GetPendingAsync_ReturnsPendingEnOrdenDeInsercion()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        await queue.EnqueueAsync(MakeEvent("identify"));
        await queue.EnqueueAsync(MakeEvent("enroll"));

        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();

        Assert.Equal(3, pending.Count);
        Assert.Equal("capture", pending[0].EventType);
        Assert.Equal("identify", pending[1].EventType);
        Assert.Equal("enroll", pending[2].EventType);
    }

    [Fact]
    public async Task GetPendingAsync_RespetaElLimite()
    {
        using SqliteEventQueue queue = CreateQueue();
        for (int i = 0; i < 5; i++)
            await queue.EnqueueAsync(MakeEvent("capture"));

        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync(limit: 3);

        Assert.Equal(3, pending.Count);
    }

    [Fact]
    public async Task GetPendingAsync_IncludesDeviceId()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture", deviceId: "ZK9500-99999"));

        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();

        Assert.Equal("ZK9500-99999", pending[0].DeviceId);
    }

    [Fact]
    public async Task MarkSentAsync_CambiasStatusAEnviado()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        await queue.EnqueueAsync(MakeEvent("capture"));
        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();

        await queue.MarkSentAsync(pending.Select(e => e.Id));

        Assert.Equal(0, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task MarkSentAsync_ConListaVacia_NoFalla()
    {
        using SqliteEventQueue queue = CreateQueue();

        await queue.MarkSentAsync([]);

        Assert.Equal(0, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task MarkFailedAsync_AntesDeLimite_MantieneEventoComoPending()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();
        long id = pending[0].Id;

        await queue.MarkFailedAsync(id);

        Assert.Equal(1, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task MarkFailedAsync_AlLlegarAlLimite_MarcaComoFailed()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();
        long id = pending[0].Id;

        for (int i = 0; i < 5; i++)
            await queue.MarkFailedAsync(id);

        Assert.Equal(0, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task PruneAsync_EliminaEventosEnviadosAntiguos()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();
        await queue.MarkSentAsync(pending.Select(e => e.Id));

        await queue.PruneAsync(TimeSpan.Zero);

        Assert.Equal(0, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task PruneAsync_ConservaEventosPendientes()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));

        await queue.PruneAsync(TimeSpan.Zero);

        Assert.Equal(1, await queue.GetPendingCountAsync());
    }

    [Fact]
    public async Task GetPendingAsync_NoDevuelveEventosEnviados()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        await queue.EnqueueAsync(MakeEvent("identify"));
        IReadOnlyList<BiometricEvent> all = await queue.GetPendingAsync();
        await queue.MarkSentAsync([all[0].Id]);

        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();

        Assert.Single(pending);
        Assert.Equal("identify", pending[0].EventType);
    }

    private static SqliteEventQueue CreateQueue()
    {
        string connStr = $"Data Source=testqueue_{Guid.NewGuid():N};Mode=Memory;Cache=Shared";
        return new SqliteEventQueue(connStr, NullLogger<SqliteEventQueue>.Instance);
    }

    private static BiometricEvent MakeEvent(string eventType, string? deviceId = "ZK9500-12345") => new()
    {
        EventType = eventType,
        EncryptedTemplateBase64 = Convert.ToBase64String(new byte[16]),
        EncryptedAesKeyBase64 = Convert.ToBase64String(new byte[256]),
        IvBase64 = Convert.ToBase64String(new byte[12]),
        TagBase64 = Convert.ToBase64String(new byte[16]),
        DeviceId = deviceId,
        CapturedAtUtc = DateTimeOffset.UtcNow
    };
}
