using SourceAFIS;

namespace Trazzo.Biometric.Agent.Utilities;

/// <summary>
/// Extrae un template <b>SourceAFIS</b> desde la imagen cruda del lector.
///
/// <para>El SDK de ZKTeco produce un template propietario que solo su propio motor entiende
/// (se usa para la identificación 1:N on-device). El backend, en cambio, matchea con SourceAFIS,
/// que no puede leer ese formato. Por eso lo que se envía al backend es un template SourceAFIS
/// generado aquí a partir de la misma imagen de la huella.</para>
/// </summary>
public static class SourceAfisTemplateExtractor
{
    /// <summary>DPI del sensor ZK9500 (FAP20, 500 dpi).</summary>
    public const int DefaultDpi = 500;

    /// <summary>
    /// Convierte la imagen en escala de grises (1 byte por píxel) en un template SourceAFIS
    /// serializado. Devuelve <c>null</c> si la imagen es inválida o la extracción falla.
    /// </summary>
    public static byte[]? TryExtract(byte[] grayscaleImage, int width, int height, int dpi = DefaultDpi)
    {
        if (width <= 0 || height <= 0 || grayscaleImage.Length < width * height)
        {
            return null;
        }

        try
        {
            // SourceAFIS espera la imagen cruda en escala de grises más el DPI real del sensor;
            // el DPI condiciona la estimación de frecuencia de crestas y por tanto la calidad
            // del template.
            FingerprintImageOptions options = new() { Dpi = dpi };
            FingerprintImage image = new(width, height, grayscaleImage, options);
            FingerprintTemplate template = new(image);
            return template.ToByteArray();
        }
        catch (Exception)
        {
            // Una imagen sin crestas suficientes puede hacer fallar la extracción: se trata como
            // "sin template" y el llamador decide (no se interrumpe la captura local).
            return null;
        }
    }
}
