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
    /// <summary>
    /// Template en formato <b>SourceAFIS</b> extraído de la misma imagen, para enviar al backend
    /// (que matchea con SourceAFIS). <c>Template</c> sigue siendo el de ZKFinger, usado por el
    /// motor de identificación 1:N on-device. <c>null</c> si no se pudo extraer.
    /// </summary>
    public byte[]? SourceAfisTemplate { get; init; }

    public static CapturedSample Succeeded(
        byte[] template,
        int templateSize,
        FingerprintQualityResult quality,
        FingerprintImagePayload? image,
        byte[]? sourceAfisTemplate = null)
    {
        return new CapturedSample(true, "Huella capturada correctamente.", template, templateSize, quality, image, BiometricOperationState.Completed)
        {
            SourceAfisTemplate = sourceAfisTemplate
        };
    }

    public static CapturedSample Failed(string message, BiometricOperationState finalState, FingerprintQualityResult? quality = null, FingerprintImagePayload? image = null)
    {
        return new CapturedSample(false, message, [], 0, quality, image, finalState);
    }
}
