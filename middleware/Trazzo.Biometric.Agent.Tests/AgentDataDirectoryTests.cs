using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AgentDataDirectoryTests
{
    // ProgramData hereda lectura y creación de archivos para BUILTIN\Users; el perfil de la cuenta del
    // servicio (para LocalSystem, system32\config\systemprofile) solo lo abren SYSTEM y Administradores.
    [Fact]
    public void LosDatosVivenEnElPerfilDeLaCuentaDelServicioYNoEnProgramData()
    {
        Assert.Equal(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), AgentDataDirectory.DefaultBasePath());
        Assert.NotEqual(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), AgentDataDirectory.DefaultBasePath());
    }

    [Fact]
    public void RemoveLegacyTemplates_BorraElPadronAntiguoYDejaLaCola()
    {
        string legacy = Directory.CreateDirectory(
            Path.Combine(Path.GetTempPath(), "trazzo-legacy-" + Guid.NewGuid().ToString("N"))).FullName;
        try
        {
            foreach (string name in new[] { "enrolled.db", "enrolled.db-wal", "enrolled.db-shm", "events.db" })
                File.WriteAllBytes(Path.Combine(legacy, name), [1]);

            AgentDataDirectory.RemoveLegacyTemplates(legacy, NullLogger.Instance);

            Assert.False(File.Exists(Path.Combine(legacy, "enrolled.db")));
            Assert.False(File.Exists(Path.Combine(legacy, "enrolled.db-wal")));
            Assert.False(File.Exists(Path.Combine(legacy, "enrolled.db-shm")));
            Assert.True(File.Exists(Path.Combine(legacy, "events.db")));
        }
        finally
        {
            Directory.Delete(legacy, recursive: true);
        }
    }

    [Fact]
    public void RemoveLegacyTemplates_SinDirectorioAntiguoNoHaceNada()
    {
        string missing = Path.Combine(Path.GetTempPath(), "trazzo-legacy-missing-" + Guid.NewGuid().ToString("N"));

        AgentDataDirectory.RemoveLegacyTemplates(missing, NullLogger.Instance);

        Assert.False(Directory.Exists(missing));
    }
}
