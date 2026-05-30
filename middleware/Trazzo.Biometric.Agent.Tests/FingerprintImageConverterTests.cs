using Trazzo.Biometric.Agent.Utilities;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintImageConverterTests
{
    private static readonly byte[] PngSignature = [137, 80, 78, 71, 13, 10, 26, 10];

    [Fact]
    public void ConvertGrayscaleRawToPng_CuandoAnchoEsCero_LanzaArgumentOutOfRangeException()
    {
        Assert.Throws<ArgumentOutOfRangeException>(
            () => FingerprintImageConverter.ConvertGrayscaleRawToPng(new byte[10], 0, 5));
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_CuandoAltoEsCero_LanzaArgumentOutOfRangeException()
    {
        Assert.Throws<ArgumentOutOfRangeException>(
            () => FingerprintImageConverter.ConvertGrayscaleRawToPng(new byte[10], 5, 0));
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_CuandoBufferMenorQueImagenEsperada_LanzaArgumentException()
    {
        Assert.Throws<ArgumentException>(
            () => FingerprintImageConverter.ConvertGrayscaleRawToPng(new byte[3], 5, 5));
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_ResultadoEmpiezaConFirmaPng()
    {
        byte[] buffer = new byte[10 * 10];
        FingerprintImagePayload result = FingerprintImageConverter.ConvertGrayscaleRawToPng(buffer, 10, 10);

        Assert.Equal(PngSignature, result.PngBytes[..8]);
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_MimeTypeEsImagenPng()
    {
        byte[] buffer = new byte[4 * 4];
        FingerprintImagePayload result = FingerprintImageConverter.ConvertGrayscaleRawToPng(buffer, 4, 4);

        Assert.Equal("image/png", result.MimeType);
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_Base64CoincidesConPngBytes()
    {
        byte[] buffer = new byte[8 * 8];
        FingerprintImagePayload result = FingerprintImageConverter.ConvertGrayscaleRawToPng(buffer, 8, 8);

        Assert.Equal(Convert.ToBase64String(result.PngBytes), result.Base64);
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_DataUrlTieneFormatoCorrecto()
    {
        byte[] buffer = new byte[4 * 4];
        FingerprintImagePayload result = FingerprintImageConverter.ConvertGrayscaleRawToPng(buffer, 4, 4);

        Assert.StartsWith("data:image/png;base64,", result.DataUrl);
        Assert.Equal($"data:image/png;base64,{result.Base64}", result.DataUrl);
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_ImagenDeUnPixel_ProducePngValido()
    {
        byte[] buffer = [128];
        FingerprintImagePayload result = FingerprintImageConverter.ConvertGrayscaleRawToPng(buffer, 1, 1);

        Assert.NotEmpty(result.PngBytes);
        Assert.Equal(PngSignature, result.PngBytes[..8]);
    }

    [Fact]
    public void ConvertGrayscaleRawToPng_BufferMayorQueImagenEsperada_NoLanzaExcepcion()
    {
        // Buffer más grande que width*height es válido (solo se usan los primeros width*height bytes)
        byte[] buffer = new byte[10 * 10 + 500];
        FingerprintImagePayload result = FingerprintImageConverter.ConvertGrayscaleRawToPng(buffer, 10, 10);

        Assert.NotEmpty(result.PngBytes);
    }
}
