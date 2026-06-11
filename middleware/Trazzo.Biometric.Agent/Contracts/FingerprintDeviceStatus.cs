namespace Trazzo.Biometric.Agent.Contracts;

public sealed record FingerprintDeviceStatus(
    string Type,
    bool Success,
    bool IsSdkAvailable,
    bool IsInitialized,
    bool IsDeviceOpen,
    bool IsConnected,
    int DeviceCount,
    string Message,
    DateTimeOffset CheckedAtUtc);
