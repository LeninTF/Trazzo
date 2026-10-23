using System.Security.AccessControl;
using System.Security.Principal;

namespace Trazzo.Biometric.Agent.Security;

/// <summary>
/// ProgramData\TrazzoAgent guarda el padrón de huellas, la cola offline y la caché de la clave pública.
/// ProgramData hereda lectura para BUILTIN\Users y les deja crear archivos: cualquier usuario de la PC
/// podía leer las bases o plantar un enrolled.db-wal / events.db-wal que SQLite aplica al abrirlas.
/// El directorio queda solo para el usuario del servicio, SYSTEM y Administradores, y el agente no
/// arranca si encuentra dentro algo cuyo dueño sea otro usuario.
/// </summary>
internal static class AgentDataDirectory
{
    internal static string EnsurePrivate(string? basePath = null)
    {
        string directory = Path.Combine(
            basePath ?? Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "TrazzoAgent");
        DirectoryInfo info = new(directory);
        if (!info.Exists)
        {
            PrivateSecurity().CreateDirectory(directory);
            return directory;
        }

        // Con un enlace dentro, la propagación del DACL alcanzaría su destino: se rechaza antes de tocar nada.
        List<string> foreign = [];
        CollectForeign(info, checkOwners: false, foreign);
        if (foreign.Count == 0)
        {
            // Primero se cierra y después se revisan los dueños: lo que alguien cree durante la revisión ya no entra.
            info.SetAccessControl(PrivateSecurity());
            CollectForeign(info, checkOwners: true, foreign);
        }

        if (foreign.Count > 0)
        {
            throw new InvalidOperationException(
                "El directorio de datos del agente contiene enlaces o archivos de otro usuario de la PC; un "
                + "administrador debe revisarlos y borrarlos antes de iniciar el servicio: " + string.Join(", ", foreign));
        }
        return directory;
    }

    internal static bool IsTrustedOwner(SecurityIdentifier? owner) =>
        owner is not null && TrustedPrincipals().Contains(owner);

    // El usuario del servicio es LocalSystem en producción; el desarrollador al correr en consola o en tests.
    private static List<SecurityIdentifier> TrustedPrincipals()
    {
        List<SecurityIdentifier> principals =
        [
            new(WellKnownSidType.LocalSystemSid, null),
            new(WellKnownSidType.BuiltinAdministratorsSid, null),
        ];
        SecurityIdentifier? serviceUser = WindowsIdentity.GetCurrent().User;
        if (serviceUser is not null && !principals.Contains(serviceUser))
            principals.Add(serviceUser);
        return principals;
    }

    private static DirectorySecurity PrivateSecurity()
    {
        DirectorySecurity security = new();
        security.SetAccessRuleProtection(isProtected: true, preserveInheritance: false);
        foreach (SecurityIdentifier principal in TrustedPrincipals())
        {
            security.AddAccessRule(new FileSystemAccessRule(principal, FileSystemRights.FullControl,
                InheritanceFlags.ContainerInherit | InheritanceFlags.ObjectInherit,
                PropagationFlags.None, AccessControlType.Allow));
        }
        return security;
    }

    // Recorre el árbol sin seguir enlaces: un symlink o junction siempre es ajeno; con checkOwners, también
    // lo que no sea de un dueño confiable.
    private static void CollectForeign(FileSystemInfo entry, bool checkOwners, List<string> foreign)
    {
        if (entry.Attributes.HasFlag(FileAttributes.ReparsePoint) || (checkOwners && !IsTrustedOwner(OwnerOf(entry))))
        {
            foreign.Add(entry.FullName);
            return;
        }

        if (entry is DirectoryInfo directory)
        {
            foreach (FileSystemInfo child in directory.EnumerateFileSystemInfos())
                CollectForeign(child, checkOwners, foreign);
        }
    }

    private static SecurityIdentifier? OwnerOf(FileSystemInfo entry)
    {
        FileSystemSecurity security = entry is DirectoryInfo directory
            ? directory.GetAccessControl(AccessControlSections.Owner)
            : ((FileInfo)entry).GetAccessControl(AccessControlSections.Owner);
        return security.GetOwner(typeof(SecurityIdentifier)) as SecurityIdentifier;
    }
}
