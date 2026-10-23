using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using Microsoft.Data.Sqlite;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Enrollment;

/// <summary>
/// Padrón local de huellas enroladas en SQLite. El template se cifra en reposo con DPAPI del usuario
/// del servicio (LocalSystem) y una entropía que lo liga a su fila; en otras plataformas (dev) se guarda
/// sin cifrar.
/// </summary>
public sealed class SqliteEnrolledFingerprintStore : IEnrolledFingerprintStore, IDisposable
{
    // v2: DPAPI CurrentUser + entropía de propósito, persona y dedo. Las filas sin prefijo usaban DPAPI
    // LocalMachine, que cualquier usuario de la PC podía fabricar: se ignoran y hay que volver a enrolarlas.
    private const string ProtectedFormatPrefix = "v2:";
    private const string EntropyPurpose = "Trazzo.Biometric.Agent.EnrolledTemplate.v2";

    private readonly string _connectionString;
    private readonly ILogger<SqliteEnrolledFingerprintStore> _logger;
    private readonly SqliteConnection? _keepAliveConnection;

    public SqliteEnrolledFingerprintStore(IConfiguration configuration, ILogger<SqliteEnrolledFingerprintStore> logger)
        : this(BuildConnectionString(configuration), logger)
    {
        string legacyDirectory = AgentDataDirectory.LegacyDirectory();
        if (string.IsNullOrWhiteSpace(configuration["Enrollment:LocalStorePath"]) && AgentDataDirectory.HasLegacyTemplates(legacyDirectory))
        {
            logger.LogWarning(
                "Hay un padrón de huellas de una versión anterior en {Directory} que cualquier usuario de la PC puede descifrar. "
                + "Ya no se usa: un administrador debe borrar ese directorio y volver a enrolar las huellas.",
                legacyDirectory);
        }
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
        string protectedTemplate = ProtectedFormatPrefix + Convert.ToBase64String(ProtectForService(
            fingerprint.Template, RowEntropy(fingerprint.UserReference, fingerprint.FingerIndex)));

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
        int previousFormat = 0;
        await using SqliteDataReader reader = await cmd.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            string userRef = reader.GetString(0);
            int fingerIndex = reader.GetInt32(1);
            string stored = reader.GetString(2);
            if (!stored.StartsWith(ProtectedFormatPrefix, StringComparison.Ordinal))
            {
                previousFormat++;
                continue;
            }

            byte[] protectedBytes;
            try
            {
                protectedBytes = Convert.FromBase64String(stored[ProtectedFormatPrefix.Length..]);
            }
            catch (FormatException ex)
            {
                _logger.LogWarning(ex, "Template enrolado con Base64 inválido. Usuario={UserRef}. Se omite.", userRef);
                continue;
            }

            byte[]? template = TryUnprotect(protectedBytes, userRef, fingerIndex);
            if (template is null)
                continue;

            result.Add(new EnrolledFingerprint(userRef, fingerIndex, template));
        }

        if (previousFormat > 0)
        {
            _logger.LogWarning(
                "{Count} huellas del padrón local tienen el formato anterior (DPAPI de ámbito máquina) y se ignoran: hay que volver a enrolarlas.",
                previousFormat);
        }

        return result;
    }

    private byte[]? TryUnprotect(byte[] protectedBytes, string userRef, int fingerIndex)
    {
        try
        {
            return UnprotectForService(protectedBytes, RowEntropy(userRef, fingerIndex));
        }
        catch (CryptographicException ex)
        {
            _logger.LogWarning(ex,
                "No se pudo descifrar un template enrolado (¿copiado de otra máquina o movido de fila?). Usuario={UserRef}, Dedo={FingerIndex}. Se omite.",
                userRef, fingerIndex);
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
        if (string.IsNullOrWhiteSpace(configuredPath))
            return $"Data Source={Path.Combine(AgentDataDirectory.Resolve(defaultBasePath), "enrolled.db")};Pooling=True";

        // Ruta elegida por el operador: debe ser un directorio dedicado y su ACL queda a su cargo.
        Directory.CreateDirectory(Path.GetDirectoryName(configuredPath)!);
        return $"Data Source={configuredPath};Pooling=True";
    }

    // El blob solo abre en la fila para la que se selló: movido a otra persona o a otro dedo, falla.
    private static byte[] RowEntropy(string userReference, int fingerIndex) =>
        Encoding.UTF8.GetBytes($"{EntropyPurpose}|{fingerIndex}|{userReference}");

    // DPAPI CurrentUser: el servicio corre como LocalSystem, así que solo LocalSystem (o un administrador)
    // descifra o fabrica un blob; con LocalMachine podía cualquier usuario de la PC. En no-Windows (dev)
    // se guarda sin cifrar; en producción el agente corre como Windows Service.
    private static byte[] ProtectForService(byte[] plaintext, byte[] entropy)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
#pragma warning disable CA1416
            return ProtectedData.Protect(plaintext, entropy, DataProtectionScope.CurrentUser);
#pragma warning restore CA1416
        }
        return plaintext;
    }

    private static byte[] UnprotectForService(byte[] protectedBytes, byte[] entropy)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
#pragma warning disable CA1416
            return ProtectedData.Unprotect(protectedBytes, entropy, DataProtectionScope.CurrentUser);
#pragma warning restore CA1416
        }
        return protectedBytes;
    }
}
