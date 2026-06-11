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
    [InlineData(-5, "No hay ningún dispositivo abierto.")]
    [InlineData(-6, "Operación no soportada por el SDK ZKTeco.")]
    [InlineData(-7, "Error de memoria en el SDK ZKTeco.")]
    [InlineData(-8, "Tiempo de espera agotado. Coloque el dedo en el lector.")]
    [InlineData(-9, "No hay datos disponibles en el SDK ZKTeco.")]
    [InlineData(-10, "Formato de datos inválido en el SDK ZKTeco.")]
    [InlineData(-16, "Dispositivo ocupado. Intente nuevamente.")]
    [InlineData(999, "El SDK ZKTeco devolvió el código de error 999.")]
    public void ToMessage_ReturnsExpectedMessage(int code, string expected)
    {
        Assert.Equal(expected, ZKTecoErrorMapper.ToMessage(code));
    }
}
