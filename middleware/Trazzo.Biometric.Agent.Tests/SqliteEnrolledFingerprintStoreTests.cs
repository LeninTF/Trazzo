using System.Security.AccessControl;
using System.Security.Cryptography;
using System.Security.Principal;
using Microsoft.Data.Sqlite;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Enrollment;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class SqliteEnrolledFingerprintStoreTests
{
    private static SqliteEnrolledFingerprintStore CreateStore()
    {
        // SQLite en memoria: la conexión keep-alive mantiene el esquema vivo durante el test.
        string connectionString = $"Data Source=enrolled-{Guid.NewGuid():N};Mode=Memory;Cache=Shared";
        return new SqliteEnrolledFingerprintStore(connectionString, NullLogger<SqliteEnrolledFingerprintStore>.Instance);
    }

    [Fact]
    public async Task SaveAsync_LuegoGetAll_DevuelveLaHuellaConTemplateIntacto()
    {
        using var store = CreateStore();
        byte[] template = Enumerable.Range(0, 300).Select(v => (byte)(v % 256)).ToArray();

        await store.SaveAsync(new EnrolledFingerprint("user-42", 1, template));
        var all = await store.GetAllAsync();

        var fp = Assert.Single(all);
        Assert.Equal("user-42", fp.UserReference);
        Assert.Equal(1, fp.FingerIndex);
        Assert.Equal(template, fp.Template);
    }

    [Fact]
    public async Task SaveAsync_ReEnrolarMismoUsuarioYDedo_ReemplazaElTemplate()
    {
        using var store = CreateStore();
        await store.SaveAsync(new EnrolledFingerprint("user-1", 2, [1, 2, 3]));
        await store.SaveAsync(new EnrolledFingerprint("user-1", 2, [9, 8, 7, 6]));

        var all = await store.GetAllAsync();

        var fp = Assert.Single(all);
        Assert.Equal(new byte[] { 9, 8, 7, 6 }, fp.Template);
    }

    [Fact]
    public async Task GetAllAsync_ConVariosUsuarios_DevuelveTodos()
    {
        using var store = CreateStore();
        await store.SaveAsync(new EnrolledFingerprint("user-1", 1, [1]));
        await store.SaveAsync(new EnrolledFingerprint("user-2", 1, [2]));
        await store.SaveAsync(new EnrolledFingerprint("user-1", 2, [3]));

        var all = await store.GetAllAsync();

        Assert.Equal(3, all.Count);
    }

    private static (SqliteEnrolledFingerprintStore Store, string ConnectionString) CreateStoreWithConnection()
    {
        string connectionString = $"Data Source=enrolled-{Guid.NewGuid():N};Mode=Memory;Cache=Shared";
        return (new SqliteEnrolledFingerprintStore(connectionString, NullLogger<SqliteEnrolledFingerprintStore>.Instance),
            connectionString);
    }

    private static object? Execute(string connectionString, string sql, params (string Name, object Value)[] parameters)
    {
        using SqliteConnection conn = new(connectionString);
        conn.Open();
        using SqliteCommand cmd = conn.CreateCommand();
        cmd.CommandText = sql;
        foreach (var (name, value) in parameters)
            cmd.Parameters.AddWithValue(name, value);
        return cmd.ExecuteScalar();
    }

    // dwFlags del blob DPAPI: dwVersion (4) + guidProvider (16) + dwMasterKeyVersion (4) + guidMasterKey (16).
    private const int DpapiFlagsOffset = 40;
    private const int CryptProtectLocalMachine = 0x4;

    private static bool IsMachineScoped(byte[] dpapiBlob) =>
        (BitConverter.ToInt32(dpapiBlob, DpapiFlagsOffset) & CryptProtectLocalMachine) != 0;

    [Fact]
    public async Task SaveAsync_ElTemplateNoQuedaConDpapiDeAmbitoMaquina()
    {
        Assert.True(IsMachineScoped(ProtectedData.Protect([1], null, DataProtectionScope.LocalMachine)));
        var (store, connectionString) = CreateStoreWithConnection();
        using var _ = store;

        await store.SaveAsync(new EnrolledFingerprint("user-1", 1, [1, 2, 3]));

        string raw = (string)Execute(connectionString,
            "SELECT template_protected_base64 FROM enrolled_fingerprints WHERE user_reference = 'user-1'")!;
        Assert.StartsWith("v2:", raw);
        Assert.False(IsMachineScoped(Convert.FromBase64String(raw["v2:".Length..])));
    }

    // Con DPAPI LocalMachine cualquier usuario de la PC fabricaba un blob válido y lo plantaba en el padrón.
    [Fact]
    public async Task GetAllAsync_IgnoraUnTemplateDeAmbitoMaquinaQueCualquierUsuarioPodiaFabricar()
    {
        var (store, connectionString) = CreateStoreWithConnection();
        using var _ = store;
        string planted = Convert.ToBase64String(ProtectedData.Protect([7, 7, 7], null, DataProtectionScope.LocalMachine));
        Execute(connectionString, """
            INSERT INTO enrolled_fingerprints (user_reference, finger_index, template_protected_base64, created_at_utc)
            VALUES ('victima', 1, @template, '2026-01-01T00:00:00Z')
            """, ("@template", planted));

        Assert.Empty(await store.GetAllAsync());
    }

    [Fact]
    public async Task GetAllAsync_IgnoraUnTemplateMovidoAOtraPersona()
    {
        var (store, connectionString) = CreateStoreWithConnection();
        using var _ = store;
        await store.SaveAsync(new EnrolledFingerprint("intruso", 1, [5, 5, 5]));

        Execute(connectionString, "UPDATE enrolled_fingerprints SET user_reference = 'victima'");

        Assert.Empty(await store.GetAllAsync());
    }

    [Fact]
    public async Task GetAllAsync_IgnoraUnTemplateMovidoAOtroDedo()
    {
        var (store, connectionString) = CreateStoreWithConnection();
        using var _ = store;
        await store.SaveAsync(new EnrolledFingerprint("user-1", 1, [5, 5, 5]));

        Execute(connectionString, "UPDATE enrolled_fingerprints SET finger_index = 2");

        Assert.Empty(await store.GetAllAsync());
    }

    private static readonly SecurityIdentifier BuiltinUsers = new(WellKnownSidType.BuiltinUsersSid, null);
    private static readonly SecurityIdentifier LocalSystem = new(WellKnownSidType.LocalSystemSid, null);

    // Imita C:\ProgramData: BUILTIN\Users heredan lectura y pueden crear archivos en cada subcarpeta.
    private static string NewProgramDataLikeDirectory()
    {
        string path = Path.Combine(Path.GetTempPath(), "trazzo-acl-" + Guid.NewGuid().ToString("N"));
        DirectoryInfo dir = Directory.CreateDirectory(path);
        DirectorySecurity security = dir.GetAccessControl();
        security.AddAccessRule(new FileSystemAccessRule(BuiltinUsers,
            FileSystemRights.ReadAndExecute | FileSystemRights.CreateFiles | FileSystemRights.AppendData,
            InheritanceFlags.ContainerInherit | InheritanceFlags.ObjectInherit, PropagationFlags.None, AccessControlType.Allow));
        dir.SetAccessControl(security);
        return path;
    }

    private static List<SecurityIdentifier> GrantedTo(FileSystemSecurity security) =>
        security.GetAccessRules(includeExplicit: true, includeInherited: true, typeof(SecurityIdentifier))
            .Cast<FileSystemAccessRule>()
            .Where(rule => rule.AccessControlType == AccessControlType.Allow)
            .Select(rule => (SecurityIdentifier)rule.IdentityReference)
            .ToList();

    [Fact]
    public void BuildConnectionString_ElDirectorioPorDefectoQuedaSoloParaElServicioSystemYAdministradores()
    {
        string programData = NewProgramDataLikeDirectory();
        try
        {
            SqliteEnrolledFingerprintStore.BuildConnectionString(new ConfigurationBuilder().Build(), programData);

            DirectorySecurity security = new DirectoryInfo(Path.Combine(programData, "TrazzoAgent")).GetAccessControl();
            Assert.True(security.AreAccessRulesProtected);
            List<SecurityIdentifier> granted = GrantedTo(security);
            Assert.DoesNotContain(BuiltinUsers, granted);
            Assert.Contains(LocalSystem, granted);
            Assert.Contains(WindowsIdentity.GetCurrent().User!, granted);
        }
        finally
        {
            Directory.Delete(programData, recursive: true);
        }
    }

    // Un agente anterior dejó el directorio y la base con los permisos heredados de ProgramData.
    [Fact]
    public void BuildConnectionString_EndureceElDirectorioYLaBaseQueDejoUnaVersionAnterior()
    {
        string programData = NewProgramDataLikeDirectory();
        try
        {
            string directory = Directory.CreateDirectory(Path.Combine(programData, "TrazzoAgent")).FullName;
            string database = Path.Combine(directory, "enrolled.db");
            File.WriteAllBytes(database, []);

            SqliteEnrolledFingerprintStore.BuildConnectionString(new ConfigurationBuilder().Build(), programData);

            Assert.True(new DirectoryInfo(directory).GetAccessControl().AreAccessRulesProtected);
            Assert.DoesNotContain(BuiltinUsers, GrantedTo(new DirectoryInfo(directory).GetAccessControl()));
            Assert.DoesNotContain(BuiltinUsers, GrantedTo(new FileInfo(database).GetAccessControl()));
        }
        finally
        {
            Directory.Delete(programData, recursive: true);
        }
    }
}
