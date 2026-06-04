namespace Trazzo.Biometric.Agent.ZKTeco;

public static class ZKTecoErrorMapper
{
    public static string ToMessage(int code)
    {
        return code switch
        {
            0  => "Operación completada correctamente.",
            -1 => "La operación del SDK ZKTeco falló.",
            -2 => "Parámetro inválido del SDK ZKTeco.",
            -3 => "El SDK ZKTeco no está inicializado.",
            -4 => "No se encontró ningún lector biométrico.",
            -5 => "No hay ningún dispositivo abierto.",
            -6 => "Operación no soportada por el SDK ZKTeco.",
            -7 => "Error de memoria en el SDK ZKTeco.",
            -8 => "Tiempo de espera agotado. Coloque el dedo en el lector.",
            -9 => "No hay datos disponibles en el SDK ZKTeco.",
            -10 => "Formato de datos inválido en el SDK ZKTeco.",
            -16 => "Dispositivo ocupado. Intente nuevamente.",
            _ => $"El SDK ZKTeco devolvió el código de error {code}."
        };
    }
}
