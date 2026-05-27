namespace Trazzo.Biometric.Agent.Queue;

public sealed class BiometricEvent
{
    public long Id { get; init; }
    public required string EventType { get; init; }
    public required string EncryptedTemplateBase64 { get; init; }
    public required string EncryptedAesKeyBase64 { get; init; }
    public required string IvBase64 { get; init; }
    public required string TagBase64 { get; init; }
    public string? DeviceId { get; init; }
    public required DateTimeOffset CapturedAtUtc { get; init; }
    public BiometricEventStatus Status { get; init; } = BiometricEventStatus.Pending;
    public int RetryCount { get; init; }
}

public enum BiometricEventStatus
{
    Pending,
    Sent,
    Failed
}
