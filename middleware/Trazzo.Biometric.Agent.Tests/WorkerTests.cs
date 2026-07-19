using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class WorkerTests
{
    [Fact]
    public async Task StartAsync_WithProductionConfiguration_InitializesAndLogsActiveServices()
    {
        RecordingLogger logger = new();
        WorkerScanner scanner = new() { Status = CreateStatus(connected: true, deviceCount: 1) };
        WorkerCryptography crypto = new() { IsConfigured = true };
        WorkerWebSocketServer webSocket = new();
        IConfiguration configuration = CreateConfiguration(new Dictionary<string, string?>
        {
            ["Backend:BaseUrl"] = "https://backend.example/api/v1",
            ["Agent:AllowedOrigins:0"] = "https://app.example",
            ["Agent:TenantId"] = "tenant-1",
            ["Agent:DeviceCode"] = "ZK9500-1",
            ["AutoUpdate:Enabled"] = "true",
            ["AutoUpdate:ManifestUrl"] = "https://updates.example/manifest.json",
            ["AutoUpdate:CheckIntervalMinutes"] = "15"
        });
        Worker worker = new(webSocket, scanner, crypto, configuration, logger);

        await worker.StartAsync(CancellationToken.None);
        await WaitForMessageAsync(logger, "=================================================");

        Assert.True(crypto.InitializeCalled);
        Assert.True(scanner.InitializeCalled);
        Assert.True(webSocket.StartCalled);
        Assert.Contains(logger.Messages, message => message.Contains("Cifrado RSA:       ACTIVO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Lector biometrico: CONECTADO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Cola de eventos:   ACTIVA", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("WebSocket CORS:    RESTRINGIDO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Tenant ID:         CONFIGURADO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Auto-Update:       ACTIVO", StringComparison.Ordinal));
    }

    [Fact]
    public async Task StartAsync_WithDevelopmentConfiguration_LogsInactiveServices()
    {
        RecordingLogger logger = new();
        Worker worker = new(
            new WorkerWebSocketServer(),
            new WorkerScanner { Status = CreateStatus(connected: false, deviceCount: 0) },
            new WorkerCryptography { IsConfigured = false },
            CreateConfiguration([]),
            logger);

        await worker.StartAsync(CancellationToken.None);
        await WaitForMessageAsync(logger, "=================================================");

        Assert.Contains(logger.Messages, message => message.Contains("Cifrado RSA:       INACTIVO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Lector biometrico: SIN LECTOR", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Cola de eventos:   INACTIVA", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("WebSocket CORS:    ABIERTO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Tenant ID:         NO CONFIGURADO", StringComparison.Ordinal));
        Assert.Contains(logger.Messages, message => message.Contains("Auto-Update:       DESHABILITADO", StringComparison.Ordinal));
    }

    [Fact]
    public async Task StartAsync_WhenAutoUpdateEnabledWithoutManifest_LogsWarning()
    {
        RecordingLogger logger = new();
        Worker worker = new(
            new WorkerWebSocketServer(),
            new WorkerScanner(),
            new WorkerCryptography(),
            CreateConfiguration(new Dictionary<string, string?>
            {
                ["AutoUpdate:Enabled"] = "true"
            }),
            logger);

        await worker.StartAsync(CancellationToken.None);
        await WaitForMessageAsync(logger, "=================================================");

        Assert.Contains(
            logger.Messages,
            message => message.Contains("HABILITADO pero sin ManifestUrl", StringComparison.Ordinal));
    }

    [Fact]
    public async Task StopAsync_StopsWebSocketAndCancelsWorker()
    {
        RecordingLogger logger = new();
        WorkerWebSocketServer webSocket = new() { BlockUntilCancelled = true };
        Worker worker = new(
            webSocket,
            new WorkerScanner(),
            new WorkerCryptography(),
            CreateConfiguration([]),
            logger);
        await worker.StartAsync(CancellationToken.None);

        await worker.StopAsync(CancellationToken.None);

        Assert.True(webSocket.StopCalled);
        Assert.Contains(logger.Messages, message => message.Contains("Deteniendo el agente", StringComparison.Ordinal));
    }

    private static IConfiguration CreateConfiguration(IEnumerable<KeyValuePair<string, string?>> settings)
    {
        return new ConfigurationBuilder()
            .AddInMemoryCollection(settings)
            .Build();
    }

    private static FingerprintDeviceStatus CreateStatus(bool connected, int deviceCount)
    {
        return new FingerprintDeviceStatus(
            "device.status.result",
            Success: connected,
            IsSdkAvailable: true,
            IsInitialized: true,
            IsDeviceOpen: connected,
            IsConnected: connected,
            DeviceCount: deviceCount,
            Message: connected ? "Conectado." : "Desconectado.",
            CheckedAtUtc: DateTimeOffset.UtcNow);
    }

    private static async Task WaitForMessageAsync(RecordingLogger logger, string expected)
    {
        using CancellationTokenSource timeout = new(TimeSpan.FromSeconds(5));
        while (!logger.Messages.Any(message => message.Contains(expected, StringComparison.Ordinal)))
        {
            await Task.Delay(10, timeout.Token);
        }
    }

    private sealed class WorkerWebSocketServer : IWebSocketServerService
    {
        public bool BlockUntilCancelled { get; init; }
        public bool StartCalled { get; private set; }
        public bool StopCalled { get; private set; }

        public Task StartAsync(CancellationToken cancellationToken)
        {
            StartCalled = true;
            return BlockUntilCancelled
                ? Task.Delay(Timeout.InfiniteTimeSpan, cancellationToken)
                : Task.CompletedTask;
        }

        public Task StopAsync(CancellationToken cancellationToken)
        {
            StopCalled = true;
            return Task.CompletedTask;
        }
    }

    private sealed class WorkerScanner : IBiometricScannerService
    {
        public FingerprintDeviceStatus Status { get; init; } = CreateStatus(false, 0);
        public bool InitializeCalled { get; private set; }

        public Task InitializeAsync(CancellationToken cancellationToken)
        {
            InitializeCalled = true;
            return Task.CompletedTask;
        }

        public Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken)
            => Task.FromResult(Status);

        public Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken)
            => Task.FromResult(FingerprintCaptureResult.Failed("Not used."));

        public Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken)
            => Task.FromResult(FingerprintIdentifyResult.Failed("Not used."));

        public Task<FingerprintEnrollResult> EnrollFingerprintAsync(
            Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
            CancellationToken cancellationToken,
            string? userReference = null,
            int fingerIndex = 0)
            => Task.FromResult(FingerprintEnrollResult.Failed("Not used."));

        public Task<FingerprintMatchResult> MatchFingerprintAgainstTemplatesAsync(
            IReadOnlyList<(int Index, byte[] Template)> templates,
            CancellationToken cancellationToken)
            => Task.FromResult(FingerprintMatchResult.Failed("Not used."));

        public FingerprintEnrollResult CancelEnrollment()
            => FingerprintEnrollResult.Failed("Not used.");

        public ValueTask DisposeAsync() => ValueTask.CompletedTask;
    }

    private sealed class WorkerCryptography : ICryptographyService
    {
        public bool IsConfigured { get; init; }
        public bool InitializeCalled { get; private set; }

        public Task InitializeAsync(CancellationToken cancellationToken)
        {
            InitializeCalled = true;
            return Task.CompletedTask;
        }

        public EncryptedPayload? TryEncryptTemplate(byte[] template, int templateSize) => null;
    }

    private sealed class RecordingLogger : ILogger<Worker>
    {
        public List<string> Messages { get; } = [];

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => true;

        public void Log<TState>(
            LogLevel logLevel,
            EventId eventId,
            TState state,
            Exception? exception,
            Func<TState, Exception?, string> formatter)
        {
            Messages.Add(formatter(state, exception));
        }
    }
}
