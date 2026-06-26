namespace Trazzo.Biometric.Agent.ZKTeco;

internal enum BiometricOperationState
{
    Idle,
    Capturing,
    Identifying,
    Enrolling,
    Cooldown,
    Completed,
    Rejected,
    TimedOut,
    Cancelled,
    Error
}
