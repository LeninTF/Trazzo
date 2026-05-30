using Trazzo.Biometric.Agent.Utilities;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintQualityAnalyzerTests
{
    private static readonly FingerprintQualityCriteria DefaultCriteria = new(
        MinimumForegroundCoveragePercent: 10,
        MaximumForegroundCoveragePercent: 60,
        MinimumContrastScore: 100,
        RequireCenteredFingerprint: true,
        CenterTolerancePercent: 25);

    [Fact]
    public void Analyze_CuandoBufferMenorQueImagenEsperada_RetornaFallo()
    {
        byte[] buffer = new byte[5];

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, DefaultCriteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("Imagen de huella inválida.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoAnchoEsCero_RetornaFallo()
    {
        var result = FingerprintQualityAnalyzer.Analyze(new byte[100], 0, 10, DefaultCriteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("Imagen de huella inválida.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoImagenTodoBlanca_RetornaCoberturaInsuficiente()
    {
        // Imagen totalmente blanca → sin píxeles oscuros → cobertura 0%
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)255);

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, DefaultCriteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("Área de huella insuficiente.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoCoberturaExcesivaDePixelesOscuros_RetornaAreaExcesiva()
    {
        // 70 píxeles oscuros (30) + 30 píxeles claros (200) = 70% de cobertura > 60% máximo
        // average=(70*30+30*200)/100=81, threshold=66 → 70 foreground < 66 → coverage=70%
        byte[] buffer = new byte[100];
        for (int i = 0; i < 70; i++) buffer[i] = 30;
        for (int i = 70; i < 100; i++) buffer[i] = 200;

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, DefaultCriteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("Área de huella excesiva.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoContrasteInsuficiente_RetornaContrasteInsuficiente()
    {
        // 30 píxeles oscuros (30) + 70 claros (200): contrast=170, pero usamos criterio MinContrast=200
        byte[] buffer = new byte[100];
        for (int i = 0; i < 30; i++) buffer[i] = 30;
        for (int i = 30; i < 100; i++) buffer[i] = 200;

        var criteria = DefaultCriteria with { MinimumContrastScore = 200, RequireCenteredFingerprint = false };

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("Contraste de huella insuficiente.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoHuellaNoEstaCentrada_RetornaNocentrada()
    {
        // 15 píxeles oscuros en esquina superior izquierda (filas 0-2, cols 0-4) → no centrado
        // average=(15*30+85*200)/100=174.5, threshold=159.5 → 15 foreground
        // bounding box: minX=0,minY=0,maxX=4,maxY=2 → center=(2,1) vs image_center=(5,5) → delta > tolerance
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 5; col++)
                buffer[row * 10 + col] = 30;

        var criteria = DefaultCriteria with { RequireCenteredFingerprint = true, CenterTolerancePercent = 25 };

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("La huella no está centrada.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoHuellaValidaCentrada_RetornaExito()
    {
        // 30 píxeles oscuros (30) en zona central (filas 2-6, cols 2-7)
        // average=(30*30+70*200)/100=149, threshold=134 → coverage=30% → contrast=170 ✓
        // bounding box center=(4.5,4) vs image_center=(5,5) → delta ≤ tolerance=2.5 ✓
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int row = 2; row <= 6; row++)
            for (int col = 2; col <= 7; col++)
                buffer[row * 10 + col] = 30;

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, DefaultCriteria);

        Assert.True(result.IsAcceptable);
        Assert.Equal("Calidad de huella aceptable.", result.Message);
    }

    [Fact]
    public void Analyze_CuandoHuellaValida_RetornaCoberturaYContrasteCalculadosCorrectamente()
    {
        // 30 píxeles at 30, 70 at 200 en imagen 10x10
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int row = 2; row <= 6; row++)
            for (int col = 2; col <= 7; col++)
                buffer[row * 10 + col] = 30;

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, DefaultCriteria);

        Assert.Equal(30.0, result.ForegroundCoveragePercent);
        Assert.Equal(170.0, result.ContrastScore);
    }

    [Fact]
    public void Analyze_CuandoNoRequiereCentrado_AceptaHuellaDescentrada()
    {
        // Misma imagen descentrada del test anterior, pero sin requerimiento de centrado
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 5; col++)
                buffer[row * 10 + col] = 30;

        var criteria = DefaultCriteria with { RequireCenteredFingerprint = false };

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.True(result.IsAcceptable);
        Assert.Equal("Calidad de huella aceptable.", result.Message);
    }
}
