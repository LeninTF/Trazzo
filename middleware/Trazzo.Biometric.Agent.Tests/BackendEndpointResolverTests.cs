using Microsoft.Extensions.Configuration;
using Trazzo.Biometric.Agent.Backend;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class BackendEndpointResolverTests
{
    [Fact]
    public void ResolveBiometricListUrl_WithBaseUrl_CombinesCorrectly()
    {
        IConfiguration configuration = BuildConfig("https://api.trazzo.pe/api/v1");

        string? url = BackendEndpointResolver.ResolveBiometricListUrl(configuration);

        Assert.Equal("https://api.trazzo.pe/api/v1/corehr/biometria", url);
    }

    [Fact]
    public void ResolveStartEnrollmentUrl_WithBaseUrl_CombinesCorrectly()
    {
        IConfiguration configuration = BuildConfig("https://api.trazzo.pe/api/v1");

        string? url = BackendEndpointResolver.ResolveStartEnrollmentUrl(configuration);

        Assert.Equal("https://api.trazzo.pe/api/v1/corehr/biometria/enroll/iniciar", url);
    }

    [Fact]
    public void Resolve_WhenEndpointPathIsAbsoluteUrl_ReturnsItDirectly()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Backend:BaseUrl"] = "https://api.trazzo.pe/api/v1",
                ["Backend:Endpoints:BiometricList"] = "https://custom.internal/biometric"
            })
            .Build();

        string? url = BackendEndpointResolver.ResolveBiometricListUrl(configuration);

        Assert.Equal("https://custom.internal/biometric", url);
    }

    [Fact]
    public void Resolve_WhenNoBaseUrl_ReturnsNull()
    {
        IConfiguration configuration = new ConfigurationBuilder().Build();

        string? url = BackendEndpointResolver.ResolveBiometricListUrl(configuration);

        Assert.Null(url);
    }

    [Fact]
    public void ResolveAttendanceSyncUrl_WhenLegacyQueueBackendUrl_UsesLegacy()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Backend:BaseUrl"] = "https://api.trazzo.pe/api/v1",
                ["Queue:BackendUrl"] = "https://legacy.api.com/sync"
            })
            .Build();

        string? url = BackendEndpointResolver.ResolveAttendanceSyncUrl(configuration);

        Assert.Equal("https://legacy.api.com/sync", url);
    }

    [Fact]
    public void ResolveAttendanceMarkUrl_WithBaseUrl_CombinesCorrectly()
    {
        IConfiguration configuration = BuildConfig("https://api.trazzo.pe/api/v1");

        string? url = BackendEndpointResolver.ResolveAttendanceMarkUrl(configuration);

        Assert.Equal("https://api.trazzo.pe/api/v1/asistencia/marcar", url);
    }

    [Fact]
    public void Resolve_WhenBaseUrlHasTrailingSlash_NormalizesCorrectly()
    {
        IConfiguration configuration = BuildConfig("https://api.trazzo.pe/api/v1/");

        string? url = BackendEndpointResolver.ResolveBiometricListUrl(configuration);

        Assert.Equal("https://api.trazzo.pe/api/v1/corehr/biometria", url);
    }

    private static IConfiguration BuildConfig(string baseUrl) =>
        new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["Backend:BaseUrl"] = baseUrl })
            .Build();
}
