using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

internal sealed class PlaintextGateScope : IDisposable
{
    private readonly bool _previous;

    public PlaintextGateScope(bool open)
    {
        _previous = BiometricSecurityGates.AllowPlaintextTemplateFallback;
        BiometricSecurityGates.AllowPlaintextTemplateFallback = open;
    }

    public void Dispose()
    {
        BiometricSecurityGates.AllowPlaintextTemplateFallback = _previous;
    }
}
