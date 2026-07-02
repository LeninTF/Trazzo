using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AgentTokenProtectorTests
{
    [Fact]
    public void Protect_AndUnprotect_RoundTripsToken()
    {
        string protectedToken = AgentTokenProtector.Protect("secret-token");

        Assert.StartsWith("dpapi-localmachine:", protectedToken);
        Assert.DoesNotContain("secret-token", protectedToken);
        Assert.Equal("secret-token", AgentTokenProtector.Unprotect(protectedToken));
    }

    [Fact]
    public void ResolveAgentToken_WhenProtectedTokenExists_UsesProtectedToken()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:AgentToken"] = "plain-token",
                ["Queue:AgentTokenProtected"] = AgentTokenProtector.Protect("protected-token")
            })
            .Build();

        string? token = AgentTokenProtector.ResolveAgentToken(
            configuration,
            NullLogger.Instance);

        Assert.Equal("protected-token", token);
    }

    [Fact]
    public void ResolveAgentToken_WhenProtectedTokenMissing_UsesLegacyPlainToken()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:AgentToken"] = "plain-token"
            })
            .Build();

        string? token = AgentTokenProtector.ResolveAgentToken(
            configuration,
            NullLogger.Instance);

        Assert.Equal("plain-token", token);
    }

    [Fact]
    public void ResolveAgentToken_WhenProtectedTokenInvalid_DoesNotUsePlainFallback()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:AgentToken"] = "plain-token",
                ["Queue:AgentTokenProtected"] = "dpapi-localmachine:not-base64"
            })
            .Build();

        string? token = AgentTokenProtector.ResolveAgentToken(
            configuration,
            NullLogger.Instance);

        Assert.Null(token);
    }
}
