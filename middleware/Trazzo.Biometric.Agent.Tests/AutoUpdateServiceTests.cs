using System.Net;
using System.Security.Cryptography;
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

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public async Task ExecuteAsync_WhenManifestUrlIsBlank_DoesNotFetch(string manifestUrl)
    {
        SequentialHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService(manifestUrl, httpClient, enabled: true);

        await service.StartAsync(CancellationToken.None);

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

    [Fact]
    public async Task ExecuteAsync_WhenManifestUrlIsRelative_DoesNotFetch()
    {
        SequentialHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService("/manifest.json", httpClient);

        await service.StartAsync(CancellationToken.None);

        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task ExecuteAsync_WhenInitialDelayIsCancelled_StopsWithoutFetch()
    {
        SequentialHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        AutoUpdateService service = CreateService(
            "https://updates.example.com/v.json",
            httpClient,
            delay: (_, _) => Task.FromException(new OperationCanceledException()));

        await service.StartAsync(CancellationToken.None);

        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task ExecuteAsync_RunsCheckAndStopsWhenIntervalDelayIsCancelled()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.ServiceUnavailable, "");
        using HttpClient httpClient = new(handler);
        int delayCalls = 0;
        TaskCompletionSource completed = new(TaskCreationOptions.RunContinuationsAsynchronously);
        AutoUpdateService service = CreateService(
            "https://updates.example.com/v.json",
            httpClient,
            delay: (_, _) =>
            {
                if (++delayCalls == 1)
                    return Task.CompletedTask;

                completed.TrySetResult();
                return Task.FromException(new OperationCanceledException());
            });

        await service.StartAsync(CancellationToken.None);
        await completed.Task.WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal(1, handler.CallCount);
        Assert.Equal(2, delayCalls);
    }

    [Fact]
    public async Task ExecuteAsync_WhenCheckThrows_ContinuesToIntervalDelay()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("999.0.0.0", "https://updates.example.com/a.msi", ""));
        using HttpClient httpClient = new(handler);
        int delayCalls = 0;
        TaskCompletionSource completed = new(TaskCreationOptions.RunContinuationsAsynchronously);
        AutoUpdateService service = CreateService(
            "https://updates.example.com/v.json",
            httpClient,
            delay: (_, _) =>
            {
                if (++delayCalls == 1)
                    return Task.CompletedTask;

                completed.TrySetResult();
                return Task.FromException(new OperationCanceledException());
            },
            getCurrentVersion: () => throw new InvalidOperationException("version failure"));

        await service.StartAsync(CancellationToken.None);
        await completed.Task.WaitAsync(TimeSpan.FromSeconds(5));

        Assert.Equal(2, delayCalls);
    }

    [Fact]
    public async Task ExecuteAsync_WhenCancelledDuringCheck_StopsLoop()
    {
        BlockingHttpMessageHandler handler = new();
        using HttpClient httpClient = new(handler);
        using CancellationTokenSource stopping = new();
        AutoUpdateService service = CreateService(
            "https://updates.example.com/v.json",
            httpClient,
            delay: (_, _) => Task.CompletedTask);

        await service.StartAsync(stopping.Token);
        await handler.RequestStarted.Task.WaitAsync(TimeSpan.FromSeconds(5));
        await stopping.CancelAsync();
        await service.StopAsync(CancellationToken.None);

        Assert.Equal(1, handler.CallCount);
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
    public async Task CheckAndApplyUpdateAsync_WhenDownloadUrlIsRelative_DoesNotDownload()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("999.0.0.0", "/a.msi", "abc"));
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

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenDownloadSucceeds_AppliesVerifiedMsi()
    {
        byte[] msiContent = [1, 2, 3, 4, 5];
        string expectedSha = Convert.ToHexStringLower(SHA256.HashData(msiContent));
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("2.0.0.0", "https://updates.example.com/a.msi", expectedSha));
        handler.Enqueue(HttpStatusCode.OK, msiContent);
        using HttpClient httpClient = new(handler);
        string updatesDirectory = Path.Combine(Path.GetTempPath(), $"trazzo-update-{Guid.NewGuid():N}");
        string? appliedPath = null;

        try
        {
            AutoUpdateService service = CreateService(
                "https://updates.example.com/v.json",
                httpClient,
                getCurrentVersion: () => new Version(1, 0, 0, 0),
                applyUpdate: path => appliedPath = path,
                updatesDirectory: updatesDirectory);

            await service.CheckAndApplyUpdateAsync(CancellationToken.None);

            Assert.NotNull(appliedPath);
            Assert.True(File.Exists(appliedPath));
            Assert.Equal(msiContent, await File.ReadAllBytesAsync(appliedPath));
        }
        finally
        {
            if (Directory.Exists(updatesDirectory))
                Directory.Delete(updatesDirectory, recursive: true);
        }
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenSha256IsMissing_DoesNotDownloadOrApplyUpdate()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("2.0.0.0", "https://updates.example.com/a.msi", ""));
        using HttpClient httpClient = new(handler);
        bool applied = false;
        AutoUpdateService service = CreateService(
            "https://updates.example.com/v.json",
            httpClient,
            getCurrentVersion: () => new Version(1, 0, 0, 0),
            applyUpdate: _ => applied = true);

        await service.CheckAndApplyUpdateAsync(CancellationToken.None);

        Assert.Equal(1, handler.CallCount);
        Assert.False(applied);
    }

    [Fact]
    public async Task CheckAndApplyUpdateAsync_WhenDownloadThrows_DoesNotApplyUpdate()
    {
        SequentialHttpMessageHandler handler = new();
        handler.Enqueue(HttpStatusCode.OK, SerializeManifest("2.0.0.0", "https://updates.example.com/a.msi", new string('0', 64)));
        handler.EnqueueException(new HttpRequestException("download failure"));
        using HttpClient httpClient = new(handler);
        bool applied = false;
        string updatesDirectory = Path.Combine(Path.GetTempPath(), $"trazzo-update-{Guid.NewGuid():N}");

        try
        {
            AutoUpdateService service = CreateService(
                "https://updates.example.com/v.json",
                httpClient,
                getCurrentVersion: () => new Version(1, 0, 0, 0),
                applyUpdate: _ => applied = true,
                updatesDirectory: updatesDirectory);

            await service.CheckAndApplyUpdateAsync(CancellationToken.None);

            Assert.False(applied);
        }
        finally
        {
            if (Directory.Exists(updatesDirectory))
                Directory.Delete(updatesDirectory, recursive: true);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private static AutoUpdateService CreateService(
        string? manifestUrl,
        HttpClient httpClient,
        bool enabled = true,
        Func<TimeSpan, CancellationToken, Task>? delay = null,
        Func<Version>? getCurrentVersion = null,
        Action<string>? applyUpdate = null,
        string? updatesDirectory = null)
    {
        IConfiguration config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["AutoUpdate:Enabled"] = enabled.ToString().ToLowerInvariant(),
                ["AutoUpdate:CheckIntervalMinutes"] = "60",
                ["AutoUpdate:ManifestUrl"] = manifestUrl
            })
            .Build();

        return new AutoUpdateService(
            config,
            NullLogger<AutoUpdateService>.Instance,
            httpClient,
            delay,
            getCurrentVersion,
            applyUpdate,
            updatesDirectory);
    }

    private static string SerializeManifest(string version, string downloadUrl, string sha256)
        => JsonSerializer.Serialize(new { version, downloadUrl, sha256 });
}

internal sealed class SequentialHttpMessageHandler : HttpMessageHandler
{
    private readonly Queue<Func<HttpResponseMessage>> _queue = new();
    public int CallCount { get; private set; }

    public void Enqueue(HttpStatusCode status, string content)
        => Enqueue(status, System.Text.Encoding.UTF8.GetBytes(content));

    public void Enqueue(HttpStatusCode status, byte[] content)
        => _queue.Enqueue(() => new HttpResponseMessage(status)
        {
            Content = new ByteArrayContent(content)
        });

    public void EnqueueException(Exception exception)
        => _queue.Enqueue(() => throw exception);

    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request, CancellationToken cancellationToken)
    {
        CallCount++;
        if (_queue.TryDequeue(out Func<HttpResponseMessage>? responseFactory))
        {
            return Task.FromResult(responseFactory());
        }
        return Task.FromResult(new HttpResponseMessage(HttpStatusCode.InternalServerError));
    }
}

internal sealed class ThrowingHttpMessageHandler : HttpMessageHandler
{
    public int CallCount { get; private set; }

    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request, CancellationToken cancellationToken)
    {
        CallCount++;
        return Task.FromException<HttpResponseMessage>(new HttpRequestException("Simulated network error"));
    }
}

internal sealed class BlockingHttpMessageHandler : HttpMessageHandler
{
    public TaskCompletionSource RequestStarted { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
    public int CallCount { get; private set; }

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        CallCount++;
        RequestStarted.TrySetResult();
        await Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken);
        throw new InvalidOperationException("Unreachable.");
    }
}
