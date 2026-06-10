using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Utilities;

namespace Trazzo.Biometric.Agent.ZKTeco;

internal sealed record CapturedSample(
    bool Success,
    string Message,
    byte[] Template,
    int TemplateSize,
    FingerprintQualityResult? Quality,
    FingerprintImagePayload? Image,
    BiometricOperationState FinalState)
{
    public static CapturedSample Succeeded(byte[] template, int templateSize, FingerprintQualityResult quality, FingerprintImagePayload? image)
    {
        return new CapturedSample(true, "Huella capturada correctamente.", template, templateSize, quality, image, BiometricOperationState.Completed);
    }

    public static CapturedSample Failed(string message, BiometricOperationState finalState, FingerprintQualityResult? quality = null, FingerprintImagePayload? image = null)
    {
        return new CapturedSample(false, message, [], 0, quality, image, finalState);
    }
}
