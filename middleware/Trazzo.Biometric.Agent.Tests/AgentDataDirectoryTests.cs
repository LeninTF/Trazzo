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
}
