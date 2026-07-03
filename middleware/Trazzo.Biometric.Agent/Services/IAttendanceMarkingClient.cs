using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Services;

public interface IAttendanceMarkingClient
{
    Task<bool> TryMarkAsync(
        EncryptedPayload encryptedTemplate,
        string? deviceId,
        DateTimeOffset capturedAtUtc,
        CancellationToken cancellationToken);
}
