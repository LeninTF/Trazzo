using System.Runtime.InteropServices;
using System.Security.Cryptography;
using Microsoft.Data.Sqlite;

namespace Trazzo.Biometric.Agent.Enrollment;

/// <summary>
/// Padrón local de huellas enroladas en SQLite. El template se cifra en reposo con DPAPI
/// LocalMachine (en Windows); en otras plataformas (dev) se guarda sin cifrar.
/// </summary>
public sealed class SqliteEnrolledFingerprintStore : IEnrolledFingerprintStore, IDisposable
{
    private readonly string _connectionString;
    private readonly ILogger<SqliteEnrolledFingerprintStore> _logger;
    private readonly SqliteConnection? _keepAliveConnection;

    public SqliteEnrolledFingerprintStore(IConfiguration configuration, ILogger<SqliteEnrolledFingerprintStore> logger)
        : this(BuildConnectionString(configuration), logger)
    {
    }

    internal SqliteEnrolledFingerprintStore(string connectionString, ILogger<SqliteEnrolledFingerprintStore> logger)
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

    public async Task SaveAsync(EnrolledFingerprint fingerprint, CancellationToken cancellationToken = default)
    {
        string protectedTemplate = Convert.ToBase64String(ProtectForLocalMachine(fingerprint.Template));

        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        // (user_reference, finger_index) es único: re-enrolar reemplaza el template anterior.
        cmd.CommandText = """
            INSERT INTO enrolled_fingerprints (user_reference, finger_index, template_protected_base64, created_at_utc)
            VALUES (@userRef, @fingerIndex, @template, @createdAt)
            ON CONFLICT(user_reference, finger_index)
            DO UPDATE SET template_protected_base64 = excluded.template_protected_base64,
                          created_at_utc = excluded.created_at_utc
            """;
        cmd.Parameters.AddWithValue("@userRef", fingerprint.UserReference);
        cmd.Parameters.AddWithValue("@fingerIndex", fingerprint.FingerIndex);
        cmd.Parameters.AddWithValue("@template", protectedTemplate);
        cmd.Parameters.AddWithValue("@createdAt", DateTimeOffset.UtcNow.ToString("O"));

        await cmd.ExecuteNonQueryAsync(cancellationToken);
        _logger.LogInformation(
            "Huella enrolada guardada en padrón local. Usuario={UserRef}, Dedo={FingerIndex}.",
            fingerprint.UserReference, fingerprint.FingerIndex);
    }

    public async Task<IReadOnlyList<EnrolledFingerprint>> GetAllAsync(CancellationToken cancellationToken = default)
    {
        await using SqliteConnection conn = new(_connectionString);
        await conn.OpenAsync(cancellationToken);
        await using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            SELECT user_reference, finger_index, template_protected_base64
            FROM enrolled_fingerprints
            ORDER BY id ASC
            """;

        List<EnrolledFingerprint> result = [];
        await using SqliteDataReader reader = await cmd.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            string userRef = reader.GetString(0);
            int fingerIndex = reader.GetInt32(1);
            byte[] protectedBytes;
            try
            {
                protectedBytes = Convert.FromBase64String(reader.GetString(2));
            }
            catch (FormatException ex)
            {
                _logger.LogWarning(ex, "Template enrolado con Base64 inválido. Usuario={UserRef}. Se omite.", userRef);
                continue;
            }

            byte[]? template = TryUnprotect(protectedBytes, userRef);
            if (template is null)
                continue;

            result.Add(new EnrolledFingerprint(userRef, fingerIndex, template));
        }

        return result;
    }

    private byte[]? TryUnprotect(byte[] protectedBytes, string userRef)
    {
        try
        {
            return UnprotectFromLocalMachine(protectedBytes);
        }
        catch (CryptographicException ex)
        {
            _logger.LogWarning(ex,
                "No se pudo descifrar un template enrolado (¿copiado de otra máquina?). Usuario={UserRef}. Se omite.",
                userRef);
            return null;
        }
    }

    public void Dispose() => _keepAliveConnection?.Dispose();

    private void InitializeDatabase()
    {
        using SqliteConnection conn = new(_connectionString);
        conn.Open();
        using SqliteCommand cmd = conn.CreateCommand();

        cmd.CommandText = """
            PRAGMA journal_mode=WAL;
            PRAGMA synchronous=NORMAL;
            CREATE TABLE IF NOT EXISTS enrolled_fingerprints (
                id                          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_reference              TEXT NOT NULL,
                finger_index                INTEGER NOT NULL,
                template_protected_base64   TEXT NOT NULL,
                created_at_utc              TEXT NOT NULL,
                UNIQUE(user_reference, finger_index)
            );
            """;
        cmd.ExecuteNonQuery();
    }

    internal static string BuildConnectionString(IConfiguration configuration, string? defaultBasePath = null)
    {
        string? configuredPath = configuration["Enrollment:LocalStorePath"];
        string dbPath = string.IsNullOrWhiteSpace(configuredPath)
            ? Path.Combine(
                defaultBasePath ?? Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
                "TrazzoAgent", "enrolled.db")
            : configuredPath;

        Directory.CreateDirectory(Path.GetDirectoryName(dbPath)!);
        return $"Data Source={dbPath};Pooling=True";
    }

    // DPAPI-LocalMachine: solo procesos de esta máquina descifran. En no-Windows (dev) se
    // guarda sin cifrar; en producción el agente corre como Windows Service.
    private static byte[] ProtectForLocalMachine(byte[] plaintext)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
#pragma warning disable CA1416
            return ProtectedData.Protect(plaintext, optionalEntropy: null, DataProtectionScope.LocalMachine);
#pragma warning restore CA1416
        }
        return plaintext;
    }

    private static byte[] UnprotectFromLocalMachine(byte[] protectedBytes)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
#pragma warning disable CA1416
            return ProtectedData.Unprotect(protectedBytes, optionalEntropy: null, DataProtectionScope.LocalMachine);
#pragma warning restore CA1416
        }
        return protectedBytes;
    }
}
