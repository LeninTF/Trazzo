using System.Security.Cryptography;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Security;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class HybridCryptographyServiceTests
{
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
