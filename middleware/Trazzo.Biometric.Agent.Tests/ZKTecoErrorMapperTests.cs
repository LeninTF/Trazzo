using Trazzo.Biometric.Agent.ZKTeco;

namespace Trazzo.Biometric.Agent.Tests;

public sealed class ZKTecoErrorMapperTests
{
    [Theory]
    [InlineData(0, "Operación completada correctamente.")]
    [InlineData(-1, "La operación del SDK ZKTeco falló.")]
    [InlineData(-2, "Parámetro inválido del SDK ZKTeco.")]
    [InlineData(-3, "El SDK ZKTeco no está inicializado.")]
    [InlineData(-4, "No se encontró ningún lector biométrico.")]
    [InlineData(-8, "Tiempo de espera agotado. Coloque el dedo en el lector.")]
    [InlineData(999, "El SDK ZKTeco devolvió el código de error 999.")]
    public void ToMessage_ReturnsExpectedMessage(int code, string expected)
    {
        Assert.Equal(expected, ZKTecoErrorMapper.ToMessage(code));
    }
}
