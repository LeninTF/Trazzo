using System.Security.Cryptography;
using System.Runtime.InteropServices;
using System.Text;

namespace Trazzo.Biometric.Agent.Security;

internal static class AgentTokenProtector
{
    private const string ProtectedPrefix = "dpapi-localmachine:";
    private const int CryptProtectLocalMachine = 0x4;
    private const int CryptProtectUiForbidden = 0x1;
    private static readonly byte[] Entropy =
        Encoding.UTF8.GetBytes("Trazzo.Biometric.Agent.Queue.AgentToken.v1");

    public static string Protect(string token)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            throw new ArgumentException("El token no puede estar vacio.", nameof(token));
        }

        byte[] plaintext = Encoding.UTF8.GetBytes(token);
        byte[] protectedBytes;
        try
        {
            protectedBytes = ProtectWithDpapi(plaintext);
        }
        finally
        {
            CryptographicOperations.ZeroMemory(plaintext);
        }

        return ProtectedPrefix + Convert.ToBase64String(protectedBytes);
    }

    public static string Unprotect(string protectedToken)
    {
        if (string.IsNullOrWhiteSpace(protectedToken))
        {
            throw new ArgumentException("El token protegido no puede estar vacio.", nameof(protectedToken));
        }

        string encoded = protectedToken.StartsWith(ProtectedPrefix, StringComparison.OrdinalIgnoreCase)
            ? protectedToken[ProtectedPrefix.Length..]
            : protectedToken;

        byte[] protectedBytes = Convert.FromBase64String(encoded);
        byte[] plaintext = UnprotectWithDpapi(protectedBytes);

        try
        {
            return Encoding.UTF8.GetString(plaintext);
        }
        finally
        {
            CryptographicOperations.ZeroMemory(plaintext);
        }
    }

    public static string? ResolveAgentToken(IConfiguration configuration, ILogger? logger = null)
    {
        string? protectedToken = configuration["Queue:AgentTokenProtected"];
        if (!string.IsNullOrWhiteSpace(protectedToken))
        {
            try
            {
                return Unprotect(protectedToken);
            }
            catch (Exception ex) when (ex is FormatException or CryptographicException or ArgumentException)
            {
                logger?.LogError(ex, "No se pudo descifrar Queue:AgentTokenProtected. Revise provisioning del agente.");
                return null;
            }
        }

        return configuration["Queue:AgentToken"];
    }

    private static byte[] ProtectWithDpapi(byte[] plaintext)
    {
        DataBlob dataIn = CreateBlob(plaintext);
        DataBlob entropy = CreateBlob(Entropy);
        try
        {
            if (!CryptProtectData(
                    ref dataIn,
                    "Trazzo Biometric Agent token",
                    ref entropy,
                    IntPtr.Zero,
                    IntPtr.Zero,
                    CryptProtectLocalMachine | CryptProtectUiForbidden,
                    out DataBlob dataOut))
            {
                throw new CryptographicException(Marshal.GetLastWin32Error());
            }

            return BlobToArrayAndFree(dataOut);
        }
        finally
        {
            FreeBlob(dataIn);
            FreeBlob(entropy);
        }
    }

    private static byte[] UnprotectWithDpapi(byte[] protectedBytes)
    {
        DataBlob dataIn = CreateBlob(protectedBytes);
        DataBlob entropy = CreateBlob(Entropy);
        try
        {
            if (!CryptUnprotectData(
                    ref dataIn,
                    IntPtr.Zero,
                    ref entropy,
                    IntPtr.Zero,
                    IntPtr.Zero,
                    CryptProtectUiForbidden,
                    out DataBlob dataOut))
            {
                throw new CryptographicException(Marshal.GetLastWin32Error());
            }

            return BlobToArrayAndFree(dataOut);
        }
        finally
        {
            FreeBlob(dataIn);
            FreeBlob(entropy);
        }
    }

    private static DataBlob CreateBlob(byte[] data)
    {
        IntPtr buffer = Marshal.AllocHGlobal(data.Length);
        Marshal.Copy(data, 0, buffer, data.Length);
        return new DataBlob(data.Length, buffer);
    }

    private static byte[] BlobToArrayAndFree(DataBlob blob)
    {
        try
        {
            byte[] output = new byte[blob.Length];
            Marshal.Copy(blob.Data, output, 0, blob.Length);
            return output;
        }
        finally
        {
            if (blob.Data != IntPtr.Zero)
            {
                _ = LocalFree(blob.Data);
            }
        }
    }

    private static void FreeBlob(DataBlob blob)
    {
        if (blob.Data != IntPtr.Zero)
        {
            Marshal.FreeHGlobal(blob.Data);
        }
    }

    [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CryptProtectData(
        ref DataBlob dataIn,
        string description,
        ref DataBlob optionalEntropy,
        IntPtr reserved,
        IntPtr promptStruct,
        int flags,
        out DataBlob dataOut);

    [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CryptUnprotectData(
        ref DataBlob dataIn,
        IntPtr description,
        ref DataBlob optionalEntropy,
        IntPtr reserved,
        IntPtr promptStruct,
        int flags,
        out DataBlob dataOut);

    [DllImport("kernel32.dll")]
    private static extern IntPtr LocalFree(IntPtr handle);

    [StructLayout(LayoutKind.Sequential)]
    private struct DataBlob(int length, IntPtr data)
    {
        public int Length = length;
        public IntPtr Data = data;
    }
}
