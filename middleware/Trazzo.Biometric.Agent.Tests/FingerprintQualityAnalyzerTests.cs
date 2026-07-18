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
        Assert.Equal(0.0, result.ScorePercent);
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
    public void Analyze_CuandoNoHayPixelesDeFondo_UsaPromedioGeneral()
    {
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)100);
        FingerprintQualityCriteria criteria = DefaultCriteria with { ContrastThresholdOffset = -1 };

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.Equal(100, result.ForegroundPixelCount);
        Assert.Equal(100, result.ForegroundCoveragePercent);
        Assert.Equal(0, result.ContrastScore);
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
        Assert.InRange(result.ForegroundCoveragePercent, criteria.MinimumForegroundCoveragePercent, criteria.MaximumForegroundCoveragePercent);
        Assert.True(result.ContrastScore >= criteria.MinimumContrastScore);
        Assert.InRange(result.ScorePercent, 0, 50);
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
        Assert.Equal(100.0, result.ScorePercent);
    }

    [Fact]
    public void Analyze_CuandoHuellaOcupaParteNormalDelZk9500_RetornaScoreAlto()
    {
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int row = 3; row <= 6; row++)
            for (int col = 3; col <= 7; col++)
                buffer[row * 10 + col] = 30;

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, DefaultCriteria);

        Assert.True(result.IsAcceptable);
        Assert.Equal(20.0, result.ForegroundCoveragePercent);
        Assert.InRange(result.ScorePercent, 90, 100);
    }

    [Fact]
    public void Analyze_CuandoHuellaDebilPeroCentrada_RetornaAceptable()
    {
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int row = 4; row <= 5; row++)
            for (int col = 3; col <= 6; col++)
                buffer[row * 10 + col] = 30;

        var criteria = DefaultCriteria with { MinimumForegroundCoveragePercent = 8 };

        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.True(result.IsAcceptable);
        Assert.Equal(8.0, result.ForegroundCoveragePercent);
        Assert.InRange(result.ScorePercent, 85, 100);
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

        Assert.Equal(30, result.ForegroundPixelCount);
        Assert.Equal(30.0, result.ForegroundCoveragePercent);
        Assert.Equal(170.0, result.ContrastScore);
        Assert.Equal(100.0, result.ScorePercent);
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

    [Fact]
    public void Analyze_CuandoCoberturaDebajoDeMinimoConContrasteAlto_ScoreProporcionalParcial()
    {
        // 5 pixels oscuros (0) + 95 brillantes (200) → coverage=5% < minimum=10%
        // CalculateCoverageScore path: coveragePercent(5) < safeMinimum(10) → proportional branch
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int i = 0; i < 5; i++) buffer[i] = 0;

        FingerprintQualityCriteria criteria = DefaultCriteria with { RequireCenteredFingerprint = false };
        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("Área de huella insuficiente.", result.Message);
        Assert.InRange(result.ScorePercent, 0, 50);
    }

    [Fact]
    public void Analyze_CuandoCoberturaEntreIdealYMaxima_ScoreDecrementalCapped()
    {
        // 40% coverage (between idealMaximum≈30% and maximum=60%) → decremental branch in CalculateCoverageScore
        // average=(40*50+60*200)/100=140, threshold=125 → 40 dark pixels at value 50
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)200);
        for (int i = 0; i < 40; i++) buffer[i] = 50;

        FingerprintQualityCriteria criteria = DefaultCriteria with { RequireCenteredFingerprint = false };
        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.True(result.IsAcceptable);
        Assert.InRange(result.ForegroundCoveragePercent, 10, 60);
        Assert.InRange(result.ScorePercent, 0, 100);
    }

    [Fact]
    public void Analyze_CuandoContrasteEnRangoMinimo_ScoreInterpolado()
    {
        // contrast=100 (exactly at minimum=100), targetContrast=150 → interpolation branch in CalculateContrastScore
        // 25 pixels at 120, 75 at 220 → average=195, threshold=180 → foreground=25 (coverage=25%)
        byte[] buffer = new byte[100];
        Array.Fill(buffer, (byte)220);
        for (int i = 0; i < 25; i++) buffer[i] = 120;

        FingerprintQualityCriteria criteria = DefaultCriteria with
        {
            RequireCenteredFingerprint = false,
            MinimumContrastScore = 100
        };
        var result = FingerprintQualityAnalyzer.Analyze(buffer, 10, 10, criteria);

        Assert.True(result.IsAcceptable);
        // Score must be between 75 and 100 (interpolated — not full 100 since contrast < target 150)
        Assert.InRange(result.ScorePercent, 75, 99);
    }

    [Fact]
    public void Analyze_CuandoCoherenciaDesactivada_NoRechazaSuperficieSinCrestas()
    {
        // MinimumRidgeCoherencePercent = 0 (default) → no se evalúa estructura.
        // Superficie uniforme oscura centrada: pasa como en el comportamiento previo.
        const int size = 32;
        byte[] buffer = new byte[size * size];
        Array.Fill(buffer, (byte)200);
        for (int y = 8; y < 24; y++)
            for (int x = 8; x < 24; x++)
                buffer[y * size + x] = 30;

        var criteria = new FingerprintQualityCriteria(
            MinimumForegroundCoveragePercent: 10,
            MaximumForegroundCoveragePercent: 60,
            MinimumContrastScore: 50,
            RequireCenteredFingerprint: false,
            CenterTolerancePercent: 25);

        var result = FingerprintQualityAnalyzer.Analyze(buffer, size, size, criteria);

        Assert.True(result.IsAcceptable);
        Assert.Equal(0, result.RidgeCoherencePercent);
    }

    [Fact]
    public void Analyze_CuandoCoherenciaActivaYHayCrestas_Acepta()
    {
        // Patrón de bandas horizontales (crestas/valles): orientación vertical dominante →
        // coherencia alta, como una huella real.
        const int size = 32;
        byte[] buffer = new byte[size * size];
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                buffer[y * size + x] = (byte)((y % 4) < 2 ? 30 : 200);

        var criteria = new FingerprintQualityCriteria(
            MinimumForegroundCoveragePercent: 1,
            MaximumForegroundCoveragePercent: 99,
            MinimumContrastScore: 1,
            RequireCenteredFingerprint: false,
            CenterTolerancePercent: 25,
            MinimumRidgeCoherencePercent: 35);

        var result = FingerprintQualityAnalyzer.Analyze(buffer, size, size, criteria);

        Assert.True(result.IsAcceptable);
        Assert.True(result.RidgeCoherencePercent >= 35);
    }

    [Fact]
    public void Analyze_CuandoCoherenciaActivaYNoHayCrestas_Rechaza()
    {
        // Textura pseudo-aleatoria sin orientación dominante (codo/palma/objeto):
        // pasa cobertura y contraste, pero falla la validación de estructura de crestas.
        const int size = 32;
        byte[] buffer = new byte[size * size];
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
            {
                // Hash pseudo-aleatorio (sin relación lineal con x/y) → sin orientación dominante.
                int h = (x * 374761393) + (y * 668265263);
                h = (h ^ (h >> 13)) * 1274126177;
                buffer[y * size + x] = (byte)(20 + ((h & 0x7fffffff) % 50));
            }

        var criteria = new FingerprintQualityCriteria(
            MinimumForegroundCoveragePercent: 1,
            MaximumForegroundCoveragePercent: 99,
            MinimumContrastScore: 1,
            RequireCenteredFingerprint: false,
            CenterTolerancePercent: 25,
            MinimumRidgeCoherencePercent: 35);

        var result = FingerprintQualityAnalyzer.Analyze(buffer, size, size, criteria);

        Assert.False(result.IsAcceptable);
        Assert.Equal("La superficie no tiene estructura de huella. Coloque la yema del dedo sobre el lector.", result.Message);
        Assert.True(result.RidgeCoherencePercent < 35);
    }
}
