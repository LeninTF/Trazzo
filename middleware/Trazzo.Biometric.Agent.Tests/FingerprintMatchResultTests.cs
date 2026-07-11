using Trazzo.Biometric.Agent.Contracts;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class FingerprintMatchResultTests
{
    [Fact]
    public void MatchedResult_DevuelveRegistroConCoincidencia()
    {
        FingerprintQualityResult quality = new(true, 95, 15, 50, true, "OK");
        DateTimeOffset before = DateTimeOffset.UtcNow;

        FingerprintMatchResult result = FingerprintMatchResult.MatchedResult(2, 512, quality, 5);

        DateTimeOffset after = DateTimeOffset.UtcNow;
        Assert.Equal("fingerprint.match.result", result.Type);
        Assert.True(result.Success);
        Assert.Equal("Huella verificada y coincidente.", result.Message);
        Assert.True(result.Matched);
        Assert.Equal(2, result.MatchedIndex);
        Assert.Equal(512, result.TemplateSize);
        Assert.Same(quality, result.Quality);
        Assert.Equal(5, result.TemplatesProvided);
        Assert.InRange(result.CapturedAtUtc, before, after);
    }

    [Fact]
    public void MatchedResult_CuandoQualityEsNull_DevuelveQualityNula()
    {
        FingerprintMatchResult result = FingerprintMatchResult.MatchedResult(0, 400, null, 3);

        Assert.True(result.Matched);
        Assert.Equal(0, result.MatchedIndex);
        Assert.Null(result.Quality);
        Assert.Equal(3, result.TemplatesProvided);
    }

    [Fact]
    public void MatchedResult_CuandoIndiceEsCero_DevuelveIndiceCero()
    {
        FingerprintMatchResult result = FingerprintMatchResult.MatchedResult(0, 512, null, 1);

        Assert.True(result.Matched);
        Assert.Equal(0, result.MatchedIndex);
    }

    [Fact]
    public void NoMatchResult_DevuelveRegistroSinCoincidencia()
    {
        FingerprintQualityResult quality = new(true, 90, 12, 45, true, "OK");
        DateTimeOffset before = DateTimeOffset.UtcNow;

        FingerprintMatchResult result = FingerprintMatchResult.NoMatchResult(512, quality, 3);

        DateTimeOffset after = DateTimeOffset.UtcNow;
        Assert.Equal("fingerprint.match.result", result.Type);
        Assert.True(result.Success);
        Assert.Equal("Huella capturada pero no coincide con ningún template enrolado.", result.Message);
        Assert.False(result.Matched);
        Assert.Null(result.MatchedIndex);
        Assert.Equal(512, result.TemplateSize);
        Assert.Same(quality, result.Quality);
        Assert.Equal(3, result.TemplatesProvided);
        Assert.InRange(result.CapturedAtUtc, before, after);
    }

    [Fact]
    public void NoMatchResult_CuandoQualityEsNull_DevuelveQualityNula()
    {
        FingerprintMatchResult result = FingerprintMatchResult.NoMatchResult(256, null, 1);

        Assert.False(result.Matched);
        Assert.Null(result.Quality);
        Assert.Null(result.MatchedIndex);
        Assert.Equal(256, result.TemplateSize);
        Assert.Equal(1, result.TemplatesProvided);
    }

    [Fact]
    public void NoMatchResult_CuandoCeroTemplates_DevuelveContadoCero()
    {
        FingerprintMatchResult result = FingerprintMatchResult.NoMatchResult(512, null, 0);

        Assert.True(result.Success);
        Assert.False(result.Matched);
        Assert.Equal(0, result.TemplatesProvided);
    }

    [Fact]
    public void Failed_DevuelveRegistroDeError()
    {
        DateTimeOffset before = DateTimeOffset.UtcNow;

        FingerprintMatchResult result = FingerprintMatchResult.Failed("Error de captura.");

        DateTimeOffset after = DateTimeOffset.UtcNow;
        Assert.Equal("fingerprint.match.result", result.Type);
        Assert.False(result.Success);
        Assert.Equal("Error de captura.", result.Message);
        Assert.False(result.Matched);
        Assert.Null(result.MatchedIndex);
        Assert.Equal(0, result.TemplateSize);
        Assert.Null(result.Quality);
        Assert.Equal(0, result.TemplatesProvided);
        Assert.InRange(result.CapturedAtUtc, before, after);
    }

    [Fact]
    public void Failed_CuandoMensajeVacio_DevuelveRegistroConMensajeVacio()
    {
        FingerprintMatchResult result = FingerprintMatchResult.Failed(string.Empty);

        Assert.False(result.Success);
        Assert.Equal(string.Empty, result.Message);
        Assert.Equal(0, result.TemplateSize);
        Assert.Equal(0, result.TemplatesProvided);
    }

    [Fact]
    public void Failed_SiempreDevuelveFalseEnMatched()
    {
        FingerprintMatchResult result = FingerprintMatchResult.Failed("cualquier error");

        Assert.False(result.Matched);
        Assert.Null(result.MatchedIndex);
    }

    [Fact]
    public void Record_CuandoMismosValores_SonIguales()
    {
        DateTimeOffset ts = DateTimeOffset.UtcNow;
        FingerprintMatchResult a = new("fingerprint.match.result", true, "OK", true, 1, 512, null, ts, 3);
        FingerprintMatchResult b = new("fingerprint.match.result", true, "OK", true, 1, 512, null, ts, 3);

        Assert.Equal(a, b);
    }

    [Fact]
    public void Record_CuandoValoresDiferentes_NoSonIguales()
    {
        DateTimeOffset ts = DateTimeOffset.UtcNow;
        FingerprintMatchResult a = new("fingerprint.match.result", true, "OK", true, 1, 512, null, ts, 3);
        FingerprintMatchResult b = new("fingerprint.match.result", true, "OK", true, 2, 512, null, ts, 3);

        Assert.NotEqual(a, b);
    }

    [Fact]
    public void Record_CuandoWith_DevuelveNuevoConValorActualizado()
    {
        FingerprintMatchResult original = FingerprintMatchResult.MatchedResult(0, 512, null, 1);
        FingerprintMatchResult updated = original with { MatchedIndex = 99 };

        Assert.Equal(0, original.MatchedIndex);
        Assert.Equal(99, updated.MatchedIndex);
        Assert.Equal(original.Type, updated.Type);
        Assert.Equal(original.Success, updated.Success);
    }
}
