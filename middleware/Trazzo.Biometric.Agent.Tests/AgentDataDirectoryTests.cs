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

    // ProgramData lo pueden manipular otros usuarios (junction + symlink del object manager): borrar ahí como
    // SYSTEM sería un borrado arbitrario. El servicio solo detecta el padrón antiguo; lo borra un administrador.
    [Fact]
    public void HasLegacyTemplates_DetectaElPadronAntiguoSinBorrarNada()
    {
        string legacy = Directory.CreateDirectory(
            Path.Combine(Path.GetTempPath(), "trazzo-legacy-" + Guid.NewGuid().ToString("N"))).FullName;
        try
        {
            string padron = Path.Combine(legacy, "enrolled.db");
            File.WriteAllBytes(padron, [1]);

            Assert.True(AgentDataDirectory.HasLegacyTemplates(legacy));
            Assert.True(File.Exists(padron));
        }
        finally
        {
            Directory.Delete(legacy, recursive: true);
        }
    }

    [Fact]
    public void HasLegacyTemplates_SinPadronAntiguoEsFalso()
    {
        string missing = Path.Combine(Path.GetTempPath(), "trazzo-legacy-missing-" + Guid.NewGuid().ToString("N"));

        Assert.False(AgentDataDirectory.HasLegacyTemplates(missing));
        Assert.False(Directory.Exists(missing));
    }
}
