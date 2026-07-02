using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using System.Security.Cryptography;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AgentTokenProtectorTests
{
    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void Protect_WhenTokenEmpty_ThrowsArgumentException(string empty)
    {
        Assert.Throws<ArgumentException>(() => AgentTokenProtector.Protect(empty));
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void Unprotect_WhenTokenEmpty_ThrowsArgumentException(string empty)
    {
        Assert.Throws<ArgumentException>(() => AgentTokenProtector.Unprotect(empty));
    }

    [Fact]
    public void Unprotect_WhenTokenHasNoPrefix_StillDecrypts()
    {
        string protected_ = AgentTokenProtector.Protect("raw-token");
        string withoutPrefix = protected_["dpapi-localmachine:".Length..];

        Assert.Equal("raw-token", AgentTokenProtector.Unprotect(withoutPrefix));
    }

    [Fact]
    public void ResolveAgentToken_WhenProtectedTokenIsCorruptBase64_ReturnsNull()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:AgentTokenProtected"] = "dpapi-localmachine:!!!not-base64!!!"
            })
            .Build();

        string? token = AgentTokenProtector.ResolveAgentToken(configuration, NullLogger.Instance);

        Assert.Null(token);
    }

    [Fact]
    public void ResolveAgentToken_WhenProtectedTokenIsValidBase64ButNotDpapi_ReturnsNull()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Queue:AgentTokenProtected"] = "dpapi-localmachine:" + Convert.ToBase64String(new byte[32])
            })
            .Build();

        string? token = AgentTokenProtector.ResolveAgentToken(configuration, NullLogger.Instance);

        Assert.Null(token);
    }


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
