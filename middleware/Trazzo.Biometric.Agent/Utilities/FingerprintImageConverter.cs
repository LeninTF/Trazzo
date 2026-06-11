using System.Buffers.Binary;
using System.IO.Compression;

namespace Trazzo.Biometric.Agent.Utilities;

public sealed record FingerprintImagePayload(
    byte[] PngBytes,
    string Base64,
    string MimeType,
    string DataUrl);

public static class FingerprintImageConverter
{
    private static readonly byte[] PngSignature = [137, 80, 78, 71, 13, 10, 26, 10];

    public static FingerprintImagePayload ConvertGrayscaleRawToPng(byte[] imageBuffer, int width, int height)
    {
        if (width <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(width), "El ancho de la imagen debe ser mayor que cero.");
        }

        if (height <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(height), "El alto de la imagen debe ser mayor que cero.");
        }

        int expectedSize = checked(width * height);
        if (imageBuffer.Length < expectedSize)
        {
            throw new ArgumentException("El buffer de imagen es menor que el tamaño esperado para la imagen en escala de grises.", nameof(imageBuffer));
        }

        byte[] pngBytes = WritePng(imageBuffer, width, height);
        string base64 = Convert.ToBase64String(pngBytes);
        const string mimeType = "image/png";

        return new FingerprintImagePayload(
            pngBytes,
            base64,
            mimeType,
            $"data:{mimeType};base64,{base64}");
    }

    private static byte[] WritePng(byte[] grayscale, int width, int height)
    {
        using MemoryStream png = new();
        png.Write(PngSignature);

        Span<byte> ihdr = stackalloc byte[13];
        BinaryPrimitives.WriteInt32BigEndian(ihdr[..4], width);
        BinaryPrimitives.WriteInt32BigEndian(ihdr.Slice(4, 4), height);
        ihdr[8] = 8; // Profundidad de bits.
        ihdr[9] = 0; // Tipo de color en escala de grises.
        ihdr[10] = 0; // Compresión Deflate.
        ihdr[11] = 0; // Filtrado adaptativo.
        ihdr[12] = 0; // Sin entrelazado.
        WriteChunk(png, "IHDR"u8, ihdr);

        using MemoryStream rawScanlines = new();
        for (int row = 0; row < height; row++)
        {
            rawScanlines.WriteByte(0); // Tipo de filtro: ninguno.
            rawScanlines.Write(grayscale.AsSpan(row * width, width));
        }

        using MemoryStream compressed = new();
        rawScanlines.Position = 0;
        using (ZLibStream zlib = new(compressed, CompressionLevel.Fastest, leaveOpen: true))
        {
            rawScanlines.CopyTo(zlib);
        }

        WriteChunk(png, "IDAT"u8, compressed.ToArray());
        WriteChunk(png, "IEND"u8, ReadOnlySpan<byte>.Empty);

        return png.ToArray();
    }

    private static void WriteChunk(Stream stream, ReadOnlySpan<byte> type, ReadOnlySpan<byte> data)
    {
        Span<byte> length = stackalloc byte[4];
        BinaryPrimitives.WriteInt32BigEndian(length, data.Length);
        stream.Write(length);
        stream.Write(type);
        stream.Write(data);

        uint crc = Crc32(type, data);
        Span<byte> crcBytes = stackalloc byte[4];
        BinaryPrimitives.WriteUInt32BigEndian(crcBytes, crc);
        stream.Write(crcBytes);
    }

    private static uint Crc32(ReadOnlySpan<byte> type, ReadOnlySpan<byte> data)
    {
        uint crc = 0xffffffff;
        crc = UpdateCrc(crc, type);
        crc = UpdateCrc(crc, data);
        return crc ^ 0xffffffff;
    }

    private static uint UpdateCrc(uint crc, ReadOnlySpan<byte> bytes)
    {
        foreach (byte value in bytes)
        {
            crc ^= value;
            for (int bit = 0; bit < 8; bit++)
            {
                uint mask = 0u - (crc & 1u);
                crc = (crc >> 1) ^ (0xedb88320u & mask);
            }
        }

        return crc;
    }
}
