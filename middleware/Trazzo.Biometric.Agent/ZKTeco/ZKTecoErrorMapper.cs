namespace Trazzo.Biometric.Agent.ZKTeco;

public static class ZKTecoErrorMapper
{
    public static string ToMessage(int code)
    {
        return code switch
        {
            0 => "Operación completada correctamente.",
            -1 => "La operación del SDK ZKTeco falló.",
            -2 => "Parámetro inválido del SDK ZKTeco.",
            -3 => "El SDK ZKTeco no está inicializado.",
            -4 => "No se encontró ningún lector biométrico.",
            -8 => "Tiempo de espera agotado. Coloque el dedo en el lector.",
            _ => $"El SDK ZKTeco devolvió el código de error {code}."
        };
    }
}
