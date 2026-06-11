using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintEnrollProgressTests
{
    [Fact]
    public void Create_ReturnsEnrollmentProgressPayload()
    {
        FingerprintEnrollProgress progress = FingerprintEnrollProgress.Create(2, 3, "Coloque el dedo nuevamente.");

        Assert.Equal("fingerprint.enroll.progress", progress.Type);
        Assert.Equal(2, progress.Step);
        Assert.Equal(3, progress.TotalSteps);
        Assert.Equal("Coloque el dedo nuevamente.", progress.Message);
    }
}
