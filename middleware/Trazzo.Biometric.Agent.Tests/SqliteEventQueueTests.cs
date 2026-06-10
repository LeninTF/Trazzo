using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Data.Sqlite;
using Trazzo.Biometric.Agent.Queue;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class SqliteEventQueueTests
{
    [Fact]
    public async Task Constructor_Publico_UsaRutaConfigurada()
    {
        string directory = Path.Combine(Path.GetTempPath(), $"trazzo-queue-{Guid.NewGuid():N}");
        string databasePath = Path.Combine(directory, "events.db");
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:DatabasePath"] = databasePath
            })
            .Build();

        try
        {
            using (SqliteEventQueue queue = new(configuration, NullLogger<SqliteEventQueue>.Instance))
            {
                await queue.EnqueueAsync(MakeEvent("capture"));

                Assert.True(File.Exists(databasePath));
                Assert.Equal(1, await queue.GetPendingCountAsync());
            }
        }
        finally
        {
            SqliteConnection.ClearAllPools();
            if (Directory.Exists(directory))
                Directory.Delete(directory, recursive: true);
        }
    }

    [Fact]
    public void BuildConnectionString_CuandoRutaNoConfigurada_UsaDirectorioPredeterminado()
    {
        string directory = Path.Combine(Path.GetTempPath(), $"trazzo-queue-default-{Guid.NewGuid():N}");

        try
        {
            string connectionString = SqliteEventQueue.BuildConnectionString(
                new ConfigurationBuilder().Build(),
                directory);

            Assert.Equal(
                $"Data Source={Path.Combine(directory, "TrazzoAgent", "events.db")}",
                connectionString);
            Assert.True(Directory.Exists(Path.Combine(directory, "TrazzoAgent")));
        }
        finally
        {
            if (Directory.Exists(directory))
                Directory.Delete(directory, recursive: true);
        }
    }

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
    public async Task GetPendingAsync_CuandoDeviceIdEsNulo_RetornaEventoCompleto()
    {
        using SqliteEventQueue queue = CreateQueue();
        BiometricEvent expected = MakeEvent("capture", deviceId: null);
        await queue.EnqueueAsync(expected);

        BiometricEvent actual = Assert.Single(await queue.GetPendingAsync());

        Assert.True(actual.Id > 0);
        Assert.Equal(expected.EventType, actual.EventType);
        Assert.Equal(expected.EncryptedTemplateBase64, actual.EncryptedTemplateBase64);
        Assert.Equal(expected.EncryptedAesKeyBase64, actual.EncryptedAesKeyBase64);
        Assert.Equal(expected.IvBase64, actual.IvBase64);
        Assert.Equal(expected.TagBase64, actual.TagBase64);
        Assert.Null(actual.DeviceId);
        Assert.Equal(expected.CapturedAtUtc, actual.CapturedAtUtc);
        Assert.Equal(BiometricEventStatus.Pending, actual.Status);
        Assert.Equal(0, actual.RetryCount);
    }

    [Fact]
    public async Task EnqueueAsync_RegistraTipoYDispositivo()
    {
        RecordingLogger logger = new();
        using SqliteEventQueue queue = CreateQueue(logger);

        await queue.EnqueueAsync(MakeEvent("identify", "ZK9500-42"));

        Assert.Contains(
            logger.Messages,
            message => message.Contains("identify", StringComparison.Ordinal)
                && message.Contains("ZK9500-42", StringComparison.Ordinal));
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
    public async Task PruneAsync_CuandoEliminaEventos_RegistraCantidad()
    {
        RecordingLogger logger = new();
        using SqliteEventQueue queue = CreateQueue(logger);
        await queue.EnqueueAsync(MakeEvent("capture"));
        BiometricEvent pending = Assert.Single(await queue.GetPendingAsync());
        await queue.MarkSentAsync([pending.Id]);

        await queue.PruneAsync(TimeSpan.Zero);

        Assert.Contains(
            logger.Messages,
            message => message.Contains("1 evento(s) limpiado(s)", StringComparison.Ordinal));
    }

    [Fact]
    public async Task PruneAsync_EliminaEventosFailedAntiguos()
    {
        using SqliteEventQueue queue = CreateQueue();
        await queue.EnqueueAsync(MakeEvent("capture"));
        IReadOnlyList<BiometricEvent> pending = await queue.GetPendingAsync();
        long id = pending[0].Id;

        for (int i = 0; i < 5; i++)
            await queue.MarkFailedAsync(id);

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

    private static SqliteEventQueue CreateQueue(ILogger<SqliteEventQueue>? logger = null)
    {
        string connStr = $"Data Source=testqueue_{Guid.NewGuid():N};Mode=Memory;Cache=Shared";
        return new SqliteEventQueue(connStr, logger ?? NullLogger<SqliteEventQueue>.Instance);
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

    private sealed class RecordingLogger : ILogger<SqliteEventQueue>
    {
        public List<string> Messages { get; } = [];

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => true;

        public void Log<TState>(
            LogLevel logLevel,
            EventId eventId,
            TState state,
            Exception? exception,
            Func<TState, Exception?, string> formatter)
        {
            Messages.Add(formatter(state, exception));
        }
    }
}
