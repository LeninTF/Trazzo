namespace Trazzo.Biometric.Agent.ZKTeco;

internal enum BiometricOperationState
{
    Idle,
    Capturing,
    Identifying,
    Enrolling,
    Completed,
    Rejected,
    TimedOut,
    Cancelled,
    Error
}
