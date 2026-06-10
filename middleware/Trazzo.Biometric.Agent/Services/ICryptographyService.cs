using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Services;

public interface ICryptographyService
{
    bool IsConfigured { get; }

    Task InitializeAsync(CancellationToken cancellationToken);

    EncryptedPayload? TryEncryptTemplate(byte[] template, int templateSize);
}
