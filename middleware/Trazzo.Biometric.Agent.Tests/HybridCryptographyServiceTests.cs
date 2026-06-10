using System.Security.Cryptography;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class HybridCryptographyServiceTests
{
    [Fact]
    public void Constructor_Publico_CuandoUrlNoConfigurada_CreaServicio()
    {
        using HybridCryptographyService service = new(
            new ConfigurationBuilder().Build(),
            NullLogger<HybridCryptographyService>.Instance);

        Assert.NotNull(service);
    }

    [Fact]
    public void Constructor_Publico_CuandoUrlNoEsHttps_CreaServicio()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Security:BackendPublicKeyUrl"] = "http://localhost/public-key"
            })
            .Build();
        using HybridCryptographyService service = new(
            configuration,
            NullLogger<HybridCryptographyService>.Instance);

        Assert.NotNull(service);
    }

    [Fact]
    public async Task BuildHttpFetcher_CuandoUrlEsHttps_ObtieneClaveYEnviaToken()
    {
        string publicKey = GenerateTestPublicKeyBase64();
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Security:BackendPublicKeyUrl"] = "https://localhost/public-key",
                ["Queue:AgentToken"] = "agent-token"
            })
            .Build();
        MockHttpMessageHandler handler = new()
        {
            ResponseContent = $$"""{"publicKey":"{{publicKey}}"}"""
        };
        using HttpClient httpClient = new(handler);
        Func<CancellationToken, Task<string?>> fetcher = HybridCryptographyService.BuildHttpFetcher(
            configuration,
            NullLogger<HybridCryptographyService>.Instance,
            httpClient);

        string? result = await fetcher(CancellationToken.None);

        Assert.Equal(publicKey, result);
        Assert.Equal("Bearer", handler.LastRequest?.Headers.Authorization?.Scheme);
        Assert.Equal("agent-token", handler.LastRequest?.Headers.Authorization?.Parameter);
    }

    [Fact]
    public async Task BuildHttpFetcher_CuandoNoHayToken_NoEnviaAuthorization()
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Security:BackendPublicKeyUrl"] = "https://localhost/public-key"
            })
            .Build();
        MockHttpMessageHandler handler = new()
        {
            ResponseContent = """{"publicKey":null}"""
        };
        using HttpClient httpClient = new(handler);
        Func<CancellationToken, Task<string?>> fetcher = HybridCryptographyService.BuildHttpFetcher(
            configuration,
            NullLogger<HybridCryptographyService>.Instance,
            httpClient);

        await fetcher(CancellationToken.None);

        Assert.Null(handler.LastRequest?.Headers.Authorization);
    }

    [Fact]
    public async Task IsConfigured_WhenFetcherReturnsNull_ReturnsFalse()
    {
        using HybridCryptographyService service = CreateService();

        await service.InitializeAsync(CancellationToken.None);

        Assert.False(service.IsConfigured);
    }

    [Fact]
    public async Task TryEncryptTemplate_WhenNotConfigured_ReturnsNull()
    {
        using HybridCryptographyService service = CreateService();
        await service.InitializeAsync(CancellationToken.None);
        byte[] template = [1, 2, 3, 4, 5];

        EncryptedPayload? result = service.TryEncryptTemplate(template, 5);

        Assert.Null(result);
    }

    [Fact]
    public async Task IsConfigured_WhenFetcherReturnsValidKey_ReturnsTrue()
    {
        string publicKeyBase64 = GenerateTestPublicKeyBase64();
        using HybridCryptographyService service = CreateService(publicKeyBase64);

        await service.InitializeAsync(CancellationToken.None);

        Assert.True(service.IsConfigured);
    }

    [Fact]
    public async Task TryEncryptTemplate_WhenConfigured_ReturnsNonNullPayload()
    {
        string publicKeyBase64 = GenerateTestPublicKeyBase64();
        using HybridCryptographyService service = CreateService(publicKeyBase64);
        await service.InitializeAsync(CancellationToken.None);
        byte[] template = [10, 20, 30, 40, 50];

        EncryptedPayload? result = service.TryEncryptTemplate(template, 5);

        Assert.NotNull(result);
        Assert.False(string.IsNullOrEmpty(result.EncryptedTemplateBase64));
        Assert.False(string.IsNullOrEmpty(result.EncryptedAesKeyBase64));
        Assert.False(string.IsNullOrEmpty(result.IvBase64));
        Assert.False(string.IsNullOrEmpty(result.TagBase64));
    }

    [Fact]
    public async Task TryEncryptTemplate_WhenConfigured_IvAndTagAreDifferentEachCall()
    {
        string publicKeyBase64 = GenerateTestPublicKeyBase64();
        using HybridCryptographyService service = CreateService(publicKeyBase64);
        await service.InitializeAsync(CancellationToken.None);
        byte[] template = [1, 2, 3, 4, 5];

        EncryptedPayload? first = service.TryEncryptTemplate(template, 5);
        EncryptedPayload? second = service.TryEncryptTemplate(template, 5);

        Assert.NotNull(first);
        Assert.NotNull(second);
        Assert.NotEqual(first.IvBase64, second.IvBase64);
        Assert.NotEqual(first.EncryptedTemplateBase64, second.EncryptedTemplateBase64);
    }

    [Fact]
    public async Task TryEncryptTemplate_CiphertextLengthMatchesTemplateSize()
    {
        string publicKeyBase64 = GenerateTestPublicKeyBase64();
        using HybridCryptographyService service = CreateService(publicKeyBase64);
        await service.InitializeAsync(CancellationToken.None);
        byte[] template = new byte[512];
        Random.Shared.NextBytes(template);

        EncryptedPayload? result = service.TryEncryptTemplate(template, 512);

        Assert.NotNull(result);
        byte[] ciphertext = Convert.FromBase64String(result.EncryptedTemplateBase64);
        Assert.Equal(512, ciphertext.Length);
    }

    [Fact]
    public async Task IsConfigured_WhenFetcherReturnsInvalidBase64_ReturnsFalse()
    {
        using HybridCryptographyService service = CreateService("not-valid-base64!!!");

        await service.InitializeAsync(CancellationToken.None);

        Assert.False(service.IsConfigured);
    }

    [Fact]
    public async Task InitializeAsync_WhenFetcherThrows_FallsBackToUnconfigured()
    {
        Func<CancellationToken, Task<string?>> failingFetcher =
            _ => Task.FromException<string?>(new HttpRequestException("Simulated network error"));

        using HybridCryptographyService service = new(
            failingFetcher,
            NullLogger<HybridCryptographyService>.Instance,
            NoCache());

        await service.InitializeAsync(CancellationToken.None);

        Assert.False(service.IsConfigured);
    }

    [Fact]
    public async Task InitializeAsync_WhenFetcherSucceeds_DoesNotLeakOldKey()
    {
        string publicKeyBase64 = GenerateTestPublicKeyBase64();
        int callCount = 0;
        Func<CancellationToken, Task<string?>> fetcher = _ =>
        {
            callCount++;
            return Task.FromResult<string?>(publicKeyBase64);
        };

        using HybridCryptographyService service = new(fetcher, NullLogger<HybridCryptographyService>.Instance);
        await service.InitializeAsync(CancellationToken.None);

        Assert.True(service.IsConfigured);
        Assert.Equal(1, callCount);
    }

    [Fact]
    public async Task InitializeAsync_WhenCacheExists_LoadsCachedKey()
    {
        string cachePath = Path.Combine(Path.GetTempPath(), $"{Guid.NewGuid():N}.cache");
        await File.WriteAllTextAsync(cachePath, GenerateTestPublicKeyBase64());

        try
        {
            using HybridCryptographyService service = new(
                _ => Task.FromResult<string?>(null),
                NullLogger<HybridCryptographyService>.Instance,
                cachePath);

            await service.InitializeAsync(CancellationToken.None);

            Assert.True(service.IsConfigured);
        }
        finally
        {
            File.Delete(cachePath);
        }
    }

    [Fact]
    public async Task InitializeAsync_WhenCacheCannotBeRead_RemainsUnconfigured()
    {
        string cachePath = Path.Combine(Path.GetTempPath(), $"{Guid.NewGuid():N}.cache");
        await File.WriteAllTextAsync(cachePath, GenerateTestPublicKeyBase64());

        try
        {
            await using FileStream lockStream = new(cachePath, FileMode.Open, FileAccess.Read, FileShare.None);
            using HybridCryptographyService service = new(
                _ => Task.FromResult<string?>(null),
                NullLogger<HybridCryptographyService>.Instance,
                cachePath);

            await service.InitializeAsync(CancellationToken.None);

            Assert.False(service.IsConfigured);
        }
        finally
        {
            File.Delete(cachePath);
        }
    }

    [Fact]
    public async Task InitializeAsync_WhenCacheCannotBeWritten_KeepsKeyInMemory()
    {
        string cachePath = Path.Combine(Path.GetTempPath(), $"trazzo-cache-{Guid.NewGuid():N}");
        Directory.CreateDirectory(cachePath);

        try
        {
            using HybridCryptographyService service = new(
                _ => Task.FromResult<string?>(GenerateTestPublicKeyBase64()),
                NullLogger<HybridCryptographyService>.Instance,
                cachePath);

            await service.InitializeAsync(CancellationToken.None);

            Assert.True(service.IsConfigured);
        }
        finally
        {
            Directory.Delete(cachePath);
        }
    }

    [Fact]
    public async Task TryRefreshKeyAsync_WhenFetcherReturnsKey_UpdatesKey()
    {
        string firstKey = GenerateTestPublicKeyBase64();
        string secondKey = GenerateTestPublicKeyBase64();
        int callCount = 0;
        using HybridCryptographyService service = new(
            _ => Task.FromResult<string?>(++callCount == 1 ? firstKey : secondKey),
            NullLogger<HybridCryptographyService>.Instance,
            NoCache());
        await service.InitializeAsync(CancellationToken.None);

        await service.TryRefreshKeyAsync();

        Assert.Equal(2, callCount);
        Assert.True(service.IsConfigured);
    }

    [Fact]
    public async Task TryRefreshKeyAsync_WhenFetcherThrows_KeepsPreviousKey()
    {
        string publicKey = GenerateTestPublicKeyBase64();
        int callCount = 0;
        using HybridCryptographyService service = new(
            _ => ++callCount == 1
                ? Task.FromResult<string?>(publicKey)
                : Task.FromException<string?>(new HttpRequestException("refresh failed")),
            NullLogger<HybridCryptographyService>.Instance,
            NoCache());
        await service.InitializeAsync(CancellationToken.None);

        await service.TryRefreshKeyAsync();

        Assert.True(service.IsConfigured);
        Assert.Equal(2, callCount);
    }

    [Fact]
    public async Task TryRefreshKeyAsync_AfterDispose_DoesNotFetch()
    {
        int callCount = 0;
        HybridCryptographyService service = new(
            _ =>
            {
                callCount++;
                return Task.FromResult<string?>(null);
            },
            NullLogger<HybridCryptographyService>.Instance,
            NoCache());
        service.Dispose();

        await service.TryRefreshKeyAsync();

        Assert.Equal(0, callCount);
    }

    [Fact]
    public async Task InitializeAsync_WhenRefreshTimerFires_FetchesAgain()
    {
        string publicKey = GenerateTestPublicKeyBase64();
        TaskCompletionSource refreshed = new(TaskCreationOptions.RunContinuationsAsynchronously);
        int callCount = 0;
        using HybridCryptographyService service = new(
            _ =>
            {
                if (Interlocked.Increment(ref callCount) >= 2)
                    refreshed.TrySetResult();
                return Task.FromResult<string?>(publicKey);
            },
            NullLogger<HybridCryptographyService>.Instance,
            NoCache(),
            refreshInterval: TimeSpan.FromMilliseconds(20));

        await service.InitializeAsync(CancellationToken.None);
        await refreshed.Task.WaitAsync(TimeSpan.FromSeconds(5));

        Assert.True(callCount >= 2);
    }

    private static HybridCryptographyService CreateService(string? keyBase64 = null)
    {
        Func<CancellationToken, Task<string?>> fetcher = _ => Task.FromResult(keyBase64);
        return new HybridCryptographyService(fetcher, NullLogger<HybridCryptographyService>.Instance, NoCache());
    }

    // Returns a path that does not exist so tests cannot read from or write to disk cache
    private static string NoCache() =>
        Path.Combine(Path.GetTempPath(), Guid.NewGuid().ToString("N"), "test.cache");

    private static string GenerateTestPublicKeyBase64()
    {
        using RSA rsa = RSA.Create(2048);
        return Convert.ToBase64String(rsa.ExportSubjectPublicKeyInfo());
    }
}
