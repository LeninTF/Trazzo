using Microsoft.Data.Sqlite;
using System.Globalization;

namespace Trazzo.Biometric.Agent.Queue;

public sealed class SqliteEventQueue : IEventQueue, IDisposable
{
    private const int MaxRetries = 5;

    private readonly string _connectionString;
    private readonly ILogger<SqliteEventQueue> _logger;
    private readonly SqliteConnection? _keepAliveConnection;

    public SqliteEventQueue(IConfiguration configuration, ILogger<SqliteEventQueue> logger)
        : this(BuildConnectionString(configuration), logger)
    {
    }

    internal SqliteEventQueue(string connectionString, ILogger<SqliteEventQueue> logger)
    {
        _connectionString = connectionString;
        _logger = logger;

        if (connectionString.Contains("Mode=Memory", StringComparison.OrdinalIgnoreCase))
        {
            _keepAliveConnection = new SqliteConnection(connectionString);
            _keepAliveConnection.Open();
        }

        InitializeDatabase();
    }

    public async Task EnqueueAsync(BiometricEvent biometricEvent, CancellationToken cancellationToken = default)
    {
        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            INSERT INTO biometric_events
                (event_type, encrypted_template_base64, encrypted_aes_key_base64,
                 iv_base64, tag_base64, device_id, captured_at_utc, status, retry_count, created_at_utc)
            VALUES
                (@eventType, @encTemplate, @encKey, @iv, @tag,
                 @deviceId, @capturedAt, 'pending', 0, @createdAt)
            """;

        cmd.Parameters.AddWithValue("@eventType", biometricEvent.EventType);
        cmd.Parameters.AddWithValue("@encTemplate", biometricEvent.EncryptedTemplateBase64);
        cmd.Parameters.AddWithValue("@encKey", biometricEvent.EncryptedAesKeyBase64);
        cmd.Parameters.AddWithValue("@iv", biometricEvent.IvBase64);
        cmd.Parameters.AddWithValue("@tag", biometricEvent.TagBase64);
        cmd.Parameters.AddWithValue("@deviceId", (object?)biometricEvent.DeviceId ?? DBNull.Value);
        cmd.Parameters.AddWithValue("@capturedAt", biometricEvent.CapturedAtUtc.ToString("O"));
        cmd.Parameters.AddWithValue("@createdAt", DateTimeOffset.UtcNow.ToString("O"));

        await cmd.ExecuteNonQueryAsync(cancellationToken);

        string eventType = biometricEvent.EventType;
        string? deviceId = biometricEvent.DeviceId;
        _logger.LogDebug(
            "Evento biométrico encolado. Tipo: {EventType}, Dispositivo: {DeviceId}.",
            eventType, deviceId);
    }

    public async Task<IReadOnlyList<BiometricEvent>> GetPendingAsync(
        int limit = 50, CancellationToken cancellationToken = default)
    {
        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            SELECT id, event_type, encrypted_template_base64, encrypted_aes_key_base64,
                   iv_base64, tag_base64, device_id, captured_at_utc, status, retry_count
            FROM biometric_events
            WHERE status = 'pending'
            ORDER BY id ASC
            LIMIT @limit
            """;
        cmd.Parameters.AddWithValue("@limit", limit);

        List<BiometricEvent> events = [];
        await using SqliteDataReader reader = await cmd.ExecuteReaderAsync(cancellationToken);

        while (await reader.ReadAsync(cancellationToken))
        {
            events.Add(new BiometricEvent
            {
                Id = reader.GetInt64(0),
                EventType = reader.GetString(1),
                EncryptedTemplateBase64 = reader.GetString(2),
                EncryptedAesKeyBase64 = reader.GetString(3),
                IvBase64 = reader.GetString(4),
                TagBase64 = reader.GetString(5),
                DeviceId = reader.IsDBNull(6) ? null : reader.GetString(6),
                CapturedAtUtc = DateTimeOffset.Parse(reader.GetString(7), CultureInfo.InvariantCulture),
                Status = Enum.Parse<BiometricEventStatus>(reader.GetString(8), ignoreCase: true),
                RetryCount = reader.GetInt32(9)
            });
        }

        return events;
    }

    public async Task MarkSentAsync(IEnumerable<long> ids, CancellationToken cancellationToken = default)
    {
        long[] idArray = [.. ids];
        if (idArray.Length == 0) return;

        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteTransaction tx = conn.BeginTransaction();
        string sentAt = DateTimeOffset.UtcNow.ToString("O");

        foreach (long id in idArray)
        {
            await using SqliteCommand cmd = conn.CreateCommand();
            cmd.Transaction = tx;
            cmd.CommandText = """
                UPDATE biometric_events
                SET status = 'sent', sent_at_utc = @sentAt
                WHERE id = @id
                """;
            cmd.Parameters.AddWithValue("@sentAt", sentAt);
            cmd.Parameters.AddWithValue("@id", id);
            await cmd.ExecuteNonQueryAsync(cancellationToken);
        }

        await tx.CommitAsync(cancellationToken);
    }

    public async Task MarkFailedAsync(long id, CancellationToken cancellationToken = default)
    {
        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            UPDATE biometric_events
            SET retry_count = retry_count + 1,
                status = CASE WHEN retry_count + 1 >= @maxRetries THEN 'failed' ELSE 'pending' END
            WHERE id = @id
            """;
        cmd.Parameters.AddWithValue("@id", id);
        cmd.Parameters.AddWithValue("@maxRetries", MaxRetries);

        await cmd.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<int> GetPendingCountAsync(CancellationToken cancellationToken = default)
    {
        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = "SELECT COUNT(*) FROM biometric_events WHERE status = 'pending'";
        object? result = await cmd.ExecuteScalarAsync(cancellationToken);
        return Convert.ToInt32(result);
    }

    public async Task PruneAsync(TimeSpan maxAge, CancellationToken cancellationToken = default)
    {
        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            DELETE FROM biometric_events
            WHERE (status = 'sent' AND sent_at_utc < @cutoff)
               OR (status = 'failed' AND created_at_utc < @cutoff)
            """;
        cmd.Parameters.AddWithValue("@cutoff", (DateTimeOffset.UtcNow - maxAge).ToString("O"));

        int deleted = await cmd.ExecuteNonQueryAsync(cancellationToken);
        if (deleted > 0)
            _logger.LogInformation("Cola biométrica: {Count} evento(s) limpiado(s) (enviados/fallidos).", deleted);
    }

    public void Dispose() => _keepAliveConnection?.Dispose();

    private void InitializeDatabase()
    {
        using SqliteConnection conn = new(_connectionString);
        conn.Open();
        using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            CREATE TABLE IF NOT EXISTS biometric_events (
                id                          INTEGER PRIMARY KEY AUTOINCREMENT,
                event_type                  TEXT NOT NULL,
                encrypted_template_base64   TEXT NOT NULL,
                encrypted_aes_key_base64    TEXT NOT NULL,
                iv_base64                   TEXT NOT NULL,
                tag_base64                  TEXT NOT NULL,
                device_id                   TEXT,
                captured_at_utc             TEXT NOT NULL,
                status                      TEXT NOT NULL DEFAULT 'pending',
                retry_count                 INTEGER NOT NULL DEFAULT 0,
                created_at_utc              TEXT NOT NULL,
                sent_at_utc                 TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_biometric_events_status ON biometric_events(status);
            """;
        cmd.ExecuteNonQuery();
    }

    private static string BuildConnectionString(IConfiguration configuration)
    {
        string? configuredPath = configuration["Queue:DatabasePath"];
        string dbPath = string.IsNullOrWhiteSpace(configuredPath)
            ? Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
                "TrazzoAgent", "events.db")
            : configuredPath;

        Directory.CreateDirectory(Path.GetDirectoryName(dbPath)!);
        return $"Data Source={dbPath}";
    }
}
