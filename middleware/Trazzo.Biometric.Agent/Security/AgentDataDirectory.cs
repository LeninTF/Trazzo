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

    private static readonly string[] LegacyTemplateFiles = ["enrolled.db", "enrolled.db-wal", "enrolled.db-shm"];

    internal static string DefaultBasePath() => Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);

    internal static string Resolve(string? basePath = null)
    {
        string directory = Path.Combine(basePath ?? DefaultBasePath(), Name);
        Directory.CreateDirectory(directory);
        return directory;
    }

    internal static string LegacyDirectory() =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), Name);

    // El padrón de versiones anteriores quedó con DPAPI de ámbito máquina (lo descifra cualquier usuario de
    // la PC) y sus filas ya no se aceptan: se borra. La cola y la caché antiguas no tienen templates en claro.
    internal static void RemoveLegacyTemplates(string legacyDirectory, ILogger logger)
    {
        foreach (string name in LegacyTemplateFiles)
        {
            string path = Path.Combine(legacyDirectory, name);
            try
            {
                if (!File.Exists(path))
                    continue;
                File.Delete(path);
                logger.LogWarning("Se borró el padrón de huellas de una versión anterior ({Path}): hay que volver a enrolar.", path);
            }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
            {
                logger.LogWarning(ex, "No se pudo borrar el padrón de huellas de una versión anterior ({Path}).", path);
            }
        }
    }
}
