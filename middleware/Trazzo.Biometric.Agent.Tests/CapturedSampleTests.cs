using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Utilities;
using Trazzo.Biometric.Agent.ZKTeco;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class CapturedSampleTests
{
    [Fact]
    public void Succeeded_ReturnsCapturedSample()
    {
        byte[] template = [1, 2, 3];
        FingerprintQualityResult quality = CreateQuality();
        FingerprintImagePayload image = CreateImage();

        CapturedSample sample = CapturedSample.Succeeded(template, 3, quality, image);

        Assert.True(sample.Success);
        Assert.Equal("Huella capturada correctamente.", sample.Message);
        Assert.Same(template, sample.Template);
        Assert.Equal(3, sample.TemplateSize);
        Assert.Equal(quality, sample.Quality);
        Assert.Equal(image, sample.Image);
        Assert.Equal(BiometricOperationState.Completed, sample.FinalState);
    }

    [Fact]
    public void Failed_ReturnsFailureDetails()
    {
        FingerprintQualityResult quality = CreateQuality();
        FingerprintImagePayload image = CreateImage();

        CapturedSample sample = CapturedSample.Failed(
            "Tiempo de espera agotado.",
            BiometricOperationState.TimedOut,
            quality,
            image);

        Assert.False(sample.Success);
        Assert.Equal("Tiempo de espera agotado.", sample.Message);
        Assert.Empty(sample.Template);
        Assert.Equal(0, sample.TemplateSize);
        Assert.Equal(quality, sample.Quality);
        Assert.Equal(image, sample.Image);
        Assert.Equal(BiometricOperationState.TimedOut, sample.FinalState);
    }

    private static FingerprintQualityResult CreateQuality()
    {
        return new FingerprintQualityResult(true, 100, 30, 50, true, "Calidad aceptable.");
    }

    private static FingerprintImagePayload CreateImage()
    {
        return new FingerprintImagePayload([1, 2, 3], "base64", "image/png", "data:image/png;base64,base64");
    }
}
