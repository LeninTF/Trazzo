namespace Trazzo.Biometric.Agent.Security;

/// <summary>
/// Directorio de datos del agente (padrón de huellas, cola offline y caché de la clave pública) en el perfil
/// de la cuenta del servicio: para LocalSystem, %SystemRoot%\System32\config\systemprofile\AppData\Local,
/// que solo abren SYSTEM y Administradores. Antes vivía en ProgramData, que hereda lectura y creación de
/// archivos para BUILTIN\Users: cualquiera leía las bases o plantaba un *.db-wal que SQLite aplica al
/// abrirlas, y cerrar ese directorio desde el servicio deja carreras con enlaces y hard links.
/// </summary>
internal static class AgentDataDirectory
{
    internal const string Name = "TrazzoAgent";

    internal static string DefaultBasePath() => Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);

    internal static string Resolve(string? basePath = null)
    {
        string directory = Path.Combine(basePath ?? DefaultBasePath(), Name);
        Directory.CreateDirectory(directory);
        return directory;
    }

    internal static string LegacyDirectory() =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), Name);

    // El padrón de versiones anteriores quedó con DPAPI de ámbito máquina (lo descifra cualquier usuario de la
    // PC). El servicio no lo borra: ProgramData lo pueden manipular otros usuarios (junction + symlink del object
    // manager) y un borrado como SYSTEM ahí sería un borrado arbitrario. Solo se avisa para que lo haga un admin.
    internal static bool HasLegacyTemplates(string legacyDirectory) =>
        File.Exists(Path.Combine(legacyDirectory, "enrolled.db"));
}
