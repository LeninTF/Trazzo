using System.Net;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.Backend;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class RemoteEnrollmentServiceTests
{
    [Fact]
    public async Task TryProcessPendingEnrollmentAsync_WhenNoPendingSession_DoesNotCapture()
    {
        SequenceHttpMessageHandler handler = new(
            new HttpResponseMessage(HttpStatusCode.NoContent));
        using HttpClient httpClient = new(handler);
        CountingScannerService scanner = new();
        RemoteEnrollmentService service = CreateService(scanner, httpClient);

        bool processed = await service.TryProcessPendingEnrollmentAsync(CancellationToken.None);

        Assert.False(processed);
        Assert.Equal(0, scanner.EnrollCalls);
        Assert.Single(handler.Requests);
        Assert.Equal(HttpMethod.Get, handler.Requests[0].Method);
    }

    [Fact]
    public async Task TryProcessPendingEnrollmentAsync_WhenPendingSession_CompletesEnrollment()
    {
        string pendingJson = JsonSerializer.Serialize(new
        {
            enroll_token = "enroll-token",
            device_id = 10,
            device_code = "ZK9500-REMOTE",
            tenant_user_id = 42,
            finger_index = 2,
            expires_at = DateTimeOffset.UtcNow.AddMinutes(2)
        });
        SequenceHttpMessageHandler handler = new(
            new HttpResponseMessage(HttpStatusCode.OK) { Content = new StringContent(pendingJson) },
            new HttpResponseMessage(HttpStatusCode.Created));
        using HttpClient httpClient = new(handler);
        EncryptedPayload encrypted = new("cipher", "key", "iv", "tag");
        CountingScannerService scanner = new()
        {
            EnrollResult = FingerprintEnrollResult.Succeeded(
                [1, 2, 3],
                3,
                capturedSamples: 3,
                deviceId: "ZK9500-CAPTURED",
                encryptedTemplate: encrypted)
        };
        RemoteEnrollmentService service = CreateService(scanner, httpClient);

        bool processed = await service.TryProcessPendingEnrollmentAsync(CancellationToken.None);

        Assert.True(processed);
        Assert.Equal(1, scanner.EnrollCalls);
        Assert.Equal(2, handler.Requests.Count);
        Assert.Equal(HttpMethod.Get, handler.Requests[0].Method);
        Assert.Contains("device_code=ZK9500-CONFIG", handler.Requests[0].RequestUri);
        Assert.Equal("tenant-1", handler.Requests[0].TenantId);
        Assert.Equal("Bearer middleware-token", handler.Requests[0].Authorization);

        Assert.Equal(HttpMethod.Post, handler.Requests[1].Method);
        using JsonDocument document = JsonDocument.Parse(handler.Requests[1].Body!);
        JsonElement root = document.RootElement;
        Assert.Equal("enroll-token", root.GetProperty("enroll_token").GetString());
        Assert.Equal("ZK9500-REMOTE", root.GetProperty("device_code").GetString());
        Assert.Equal(2, root.GetProperty("finger_index").GetInt32());
        Assert.Equal("cipher", root.GetProperty("encrypted_template_base64").GetString());
        Assert.Equal("key", root.GetProperty("encrypted_aes_key_base64").GetString());
        Assert.Equal("iv", root.GetProperty("iv_base64").GetString());
        Assert.Equal("tag", root.GetProperty("tag_base64").GetString());
    }

    [Fact]
    public void BuildPendingEnrollmentUrl_AppendsEscapedDeviceCode()
    {
        string url = RemoteEnrollmentService.BuildPendingEnrollmentUrl(
            "https://api.trazzo.pe/api/v1/corehr/biometria/enroll/pendiente?x=1",
            "ZK 9500/1");

        Assert.Equal(
            "https://api.trazzo.pe/api/v1/corehr/biometria/enroll/pendiente?x=1&device_code=ZK%209500%2F1",
            url);
    }

    private static RemoteEnrollmentService CreateService(
        CountingScannerService scanner,
        HttpClient httpClient)
    {
        IConfiguration configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Backend:BaseUrl"] = "https://api.trazzo.pe/api/v1",
                ["Agent:TenantId"] = "tenant-1",
                ["Agent:DeviceCode"] = "ZK9500-CONFIG",
                ["Queue:AgentToken"] = "middleware-token",
                ["Enrollment:RemotePollingEnabled"] = "true",
                ["Enrollment:RemotePollingIntervalSeconds"] = "1"
            })
            .Build();

        return new RemoteEnrollmentService(
            scanner,
            configuration,
            NullLogger<RemoteEnrollmentService>.Instance,
            httpClient);
    }

    private sealed class CountingScannerService : IBiometricScannerService
    {
        public int EnrollCalls { get; private set; }

        public FingerprintEnrollResult EnrollResult { get; init; } =
            FingerprintEnrollResult.Failed("No configurado.");

        public Task InitializeAsync(CancellationToken cancellationToken) => Task.CompletedTask;

        public Task<FingerprintDeviceStatus> GetStatusAsync(CancellationToken cancellationToken)
        {
            return Task.FromResult(new FingerprintDeviceStatus(
                "device.status.result",
                Success: true,
                IsSdkAvailable: true,
                IsInitialized: true,
                IsDeviceOpen: true,
                IsConnected: true,
                DeviceCount: 1,
                Message: "OK",
                CheckedAtUtc: DateTimeOffset.UtcNow));
        }

        public Task<FingerprintCaptureResult> CaptureFingerprintAsync(CancellationToken cancellationToken)
            => Task.FromResult(FingerprintCaptureResult.Failed("No usado."));

        public Task<FingerprintIdentifyResult> IdentifyFingerprintAsync(CancellationToken cancellationToken)
            => Task.FromResult(FingerprintIdentifyResult.Failed("No usado."));

        public Task<FingerprintEnrollResult> EnrollFingerprintAsync(
            Func<FingerprintEnrollProgress, CancellationToken, Task> progressCallback,
            CancellationToken cancellationToken)
        {
            EnrollCalls++;
            return Task.FromResult(EnrollResult);
        }

        public FingerprintEnrollResult CancelEnrollment()
            => FingerprintEnrollResult.Failed("No usado.");

        public ValueTask DisposeAsync() => ValueTask.CompletedTask;
    }

    private sealed class SequenceHttpMessageHandler(params HttpResponseMessage[] responses) : HttpMessageHandler
    {
        private readonly Queue<HttpResponseMessage> _responses = new(responses);

        public List<CapturedRequest> Requests { get; } = [];

        protected override async Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken)
        {
            string? body = request.Content is null
                ? null
                : await request.Content.ReadAsStringAsync(cancellationToken);
            Requests.Add(new CapturedRequest(
                request.Method,
                request.RequestUri?.ToString() ?? "",
                body,
                request.Headers.Authorization is null
                    ? null
                    : $"{request.Headers.Authorization.Scheme} {request.Headers.Authorization.Parameter}",
                request.Headers.TryGetValues("X-Tenant-ID", out IEnumerable<string>? tenantValues)
                    ? tenantValues.Single()
                    : null));

            return _responses.Dequeue();
        }
    }

    private sealed record CapturedRequest(
        HttpMethod Method,
        string RequestUri,
        string? Body,
        string? Authorization,
        string? TenantId);
}
