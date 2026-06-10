using System.Net;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.AutoUpdate;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class AutoUpdateServiceTests
{
    // ─── Constructor ───────────────────────────────────────────────────────

    [Fact]
    public void Constructor_WithSharedHttpClient_DoesNotThrow()
    {
        IConfiguration config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["AutoUpdate:Enabled"] = "false"
            })
            .Build();

        AutoUpdateService service = new(config, NullLogger<AutoUpdateService>.Instance);
        Assert.NotNull(service);
    }

    // ─── ExecuteAsync early-return paths ───────────────────────────────────

    [Fact]
    public async Task ExecuteAsync_WhenDisabled_DoesNotFetch()
    {
        SequentialHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient, enabled: false);

        using CancellationTokenSource cts = new();
        await service.StartAsync(cts.Token);
        await cts.CancelAsync();
        await service.StopAsync(CancellationToken.None);

        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task ExecuteAsync_WhenManifestUrlIsEmpty_DoesNotFetch()
    {
        SequentialHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService(null, httpClient, enabled: true);

        using CancellationTokenSource cts = new();
        await service.StartAsync(cts.Token);
        await cts.CancelAsync();
        await service.StopAsync(CancellationToken.None);

        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task ExecuteAsync_WhenManifestUrlIsHttp_DoesNotFetch()
    {
        SequentialHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("http://insecure.example.com/v.json", httpClient, enabled: true);

        using CancellationTokenSource cts = new();
        await service.StartAsync(cts.Token);
        await cts.CancelAsync();
        await service.StopAsync(CancellationToken.None);

        Assert.Equal(0, handler.CallCount);
    }

    // ─── CheckAndApplyUpdateAsync business logic ───────────────────────────

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenManifestReturns503_DoesNotThrow()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.ServiceUnavailable, "");
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(1, handler.CallCount);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenManifestHasInvalidVersion_DoesNotDownload()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("not-a-version", "https://updates.example.com/a.msi", "abc"));
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(1, handler.CallCount);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenDownloadUrlIsHttp_DoesNotDownload()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("999.0.0.0", "http://insecure.example.com/a.msi", "abc"));
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(1, handler.CallCount);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenRemoteVersionIsOlderThanCurrent_DoesNotDownload()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("0.0.0.1", "https://updates.example.com/a.msi", "abc"));
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(1, handler.CallCount);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenDownloadReturns404_DoesNotApplyUpdate()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("999.0.0.0", "https://updates.example.com/a.msi", "abc"));
        handler.Enqueue(HttpStatusCode.NotFound, "");
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(2, handler.CallCount);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenSha256Mismatch_DoesNotApplyUpdate()
    {
        byte[] fakeMsiContent = [0xDE, 0xAD, 0xBE, 0xEF, 0x00];
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("999.0.0.0", "https://updates.example.com/a.msi", "wrong000000000000000000000000000000000000000000000000000000000000"));
        handler.Enqueue(HttpStatusCode.OK, fakeMsiContent);
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(2, handler.CallCount);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenNetworkThrows_DoesNotThrow()
    {
        ThrowingHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("https://updates.example.com/v.json", httpClient);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private static AutoUpdateService CreateService(string? manifestUrl, HttpClient httpClient, bool enabled = true)
    {
        IConfiguration config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["AutoUpdate:Enabled"] = enabled.ToString().ToLowerInvariant(),
                ["AutoUpdate:CheckIntervalMinutes"] = "60",
                ["AutoUpdate:ManifestUrl"] = manifestUrl
            })
            .Build();

        return new AutoUpdateService(config, NullLogger<AutoUpdateService>.Instance, httpClient);
    }

    private static string SerializeManifest(string version, string downloadUrl, string sha256)
        => JsonSerializer.Serialize(new { version, downloadUrl, sha256 });
}

internal sealed class SequentialHttpMessageHandler : HttpMessageHandler
{
    private readonly Queue<(HttpStatusCode Status, byte[] Content)> _queue = new();
    public int CallCount { get; private set; }

    public void Enqueue(HttpStatusCode status, string content)
        => _queue.Enqueue((status, System.Text.Encoding.UTF8.GetBytes(content)));

    public void Enqueue(HttpStatusCode status, byte[] content)
        => _queue.Enqueue((status, content));

    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request, CancellationToken cancellationToken)
    {
        CallCount++;
        if (_queue.TryDequeue(out (HttpStatusCode Status, byte[] Content) item))
        {
            HttpResponseMessage response = new(item.Status)
            {
                Content = new ByteArrayContent(item.Content)
            };
            return Task.FromResult(response);
        }
        return Task.FromResult(new HttpResponseMessage(HttpStatusCode.InternalServerError));
    }
}

internal sealed class ThrowingHttpMessageHandler : HttpMessageHandler
{
    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request, CancellationToken cancellationToken)
        => Task.FromException<HttpResponseMessage>(new HttpRequestException("Simulated network error"));
}
