using System.Diagnostics;
using System.Security.Principal;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AgentDataDirectoryTests
{
    // Un directorio de datos ya existente cuyo dueño no es uno de estos lo creó otro usuario (squatting):
    // el agente no arranca hasta que un administrador lo revise.
    [Fact]
    public void IsTrustedOwner_SoloSystemAdministradoresYElUsuarioDelServicio()
    {
        Assert.True(AgentDataDirectory.IsTrustedOwner(new SecurityIdentifier(WellKnownSidType.LocalSystemSid, null)));
        Assert.True(AgentDataDirectory.IsTrustedOwner(new SecurityIdentifier(WellKnownSidType.BuiltinAdministratorsSid, null)));
        Assert.True(AgentDataDirectory.IsTrustedOwner(WindowsIdentity.GetCurrent().User));

        Assert.False(AgentDataDirectory.IsTrustedOwner(new SecurityIdentifier(WellKnownSidType.BuiltinUsersSid, null)));
        Assert.False(AgentDataDirectory.IsTrustedOwner(new SecurityIdentifier("S-1-5-21-1000-2000-3000-1001")));
        Assert.False(AgentDataDirectory.IsTrustedOwner(null));
    }

    // Con un junction dentro, la propagación del DACL alcanzaría su destino y SQLite abriría lo que apunta.
    [Fact]
    public void EnsurePrivate_NoArrancaSiHayUnEnlaceEnUnSubdirectorio()
    {
        string programData = Path.Combine(Path.GetTempPath(), "trazzo-reparse-" + Guid.NewGuid().ToString("N"));
        string outside = Directory.CreateDirectory(Path.Combine(programData, "fuera")).FullName;
        string nested = Directory.CreateDirectory(Path.Combine(programData, "TrazzoAgent", "logs")).FullName;
        string link = Path.Combine(nested, "enlace");
        try
        {
            CreateJunction(link, outside);

            var error = Assert.Throws<InvalidOperationException>(() => AgentDataDirectory.EnsurePrivate(programData));
            Assert.Contains(link, error.Message);
            Assert.False(new DirectoryInfo(Path.Combine(programData, "TrazzoAgent")).GetAccessControl().AreAccessRulesProtected);
        }
        finally
        {
            if (Directory.Exists(link))
                Directory.Delete(link);
            Directory.Delete(programData, recursive: true);
        }
    }

    // mklink /J no exige privilegios, a diferencia de un symlink.
    private static void CreateJunction(string link, string target)
    {
        using Process process = Process.Start(new ProcessStartInfo("cmd.exe", $"/c mklink /J \"{link}\" \"{target}\"")
        {
            CreateNoWindow = true,
            UseShellExecute = false,
            RedirectStandardOutput = true,
        })!;
        process.WaitForExit();
        Assert.Equal(0, process.ExitCode);
    }
}
