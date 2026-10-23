using Trazzo.Biometric.Agent.Utilities;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class SourceAfisTemplateExtractorTests
{
    // Huella sintética: crestas curvas concéntricas (como el núcleo de una yema) sobre fondo claro,
    // con el tamaño real del ZK9500 (300×400). Suficiente para que SourceAFIS extraiga minucias.
    private static byte[] BuildSyntheticFingerprint(int width, int height, int period = 8)
    {
        byte[] image = new byte[width * height];
        Array.Fill(image, (byte)235);

        double centerX = width / 2.0;
        double centerY = height / 2.0;
        int radius = Math.Min(width, height) / 2 - 10;

        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                double dx = x - centerX;
                double dy = y - centerY;
                double dist = Math.Sqrt(dx * dx + dy * dy);
                if (dist > radius) continue;

                bool ridge = ((int)(dist / period)) % 2 == 0;
                image[y * width + x] = (byte)(ridge ? 40 : 210);
            }
        }

        return image;
    }

    [Fact]
    public void TryExtract_ConHuellaSintetica_DevuelveTemplateSerializado()
    {
        byte[] image = BuildSyntheticFingerprint(300, 400);

        byte[]? template = SourceAfisTemplateExtractor.TryExtract(image, 300, 400);

        Assert.NotNull(template);
        Assert.NotEmpty(template);
    }

    [Fact]
    public void TryExtract_ConDimensionesInvalidas_DevuelveNull()
    {
        Assert.Null(SourceAfisTemplateExtractor.TryExtract([1, 2, 3], 0, 10));
        Assert.Null(SourceAfisTemplateExtractor.TryExtract([1, 2, 3], 10, 0));
    }

    [Fact]
    public void TryExtract_CuandoBufferEsMenorQueLaImagen_DevuelveNull()
    {
        byte[] tooSmall = new byte[100];

        Assert.Null(SourceAfisTemplateExtractor.TryExtract(tooSmall, 300, 400));
    }

    [Fact]
    public void TryExtract_DosCapturasDeLaMismaHuella_ProducenTemplatesEquivalentes()
    {
        // Determinismo: la misma imagen debe producir el mismo template, requisito para que el
        // matching del backend sea reproducible.
        byte[] image = BuildSyntheticFingerprint(300, 400);

        byte[]? first = SourceAfisTemplateExtractor.TryExtract(image, 300, 400);
        byte[]? second = SourceAfisTemplateExtractor.TryExtract(image, 300, 400);

        Assert.NotNull(first);
        Assert.Equal(first, second);
    }
}
