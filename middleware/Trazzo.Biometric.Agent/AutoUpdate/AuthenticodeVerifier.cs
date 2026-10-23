using System.Runtime.InteropServices;
using System.Security.Cryptography.X509Certificates;

namespace Trazzo.Biometric.Agent.AutoUpdate;

/// <summary>
/// Verificación Authenticode real de un archivo firmado. <see cref="X509Certificate.CreateFromSignedFile"/>
/// solo extrae el certificado embebido y no comprueba que el digest firmado corresponda al contenido:
/// un instalador alterado al que se le copia la firma de otro archivo seguiría pasando. WinVerifyTrust
/// valida el digest, la cadena hasta una raíz de confianza y, si se pide, la revocación.
/// </summary>
internal static partial class AuthenticodeVerifier
{
    private static readonly Guid GenericVerifyV2 = new("00AAC56B-CD44-11D0-8CC2-00C04FC295EE");
    private static readonly IntPtr InvalidHandleValue = new(-1);

    private const uint WtdUiNone = 2;
    private const uint WtdRevokeNone = 0;
    private const uint WtdRevokeWholeChain = 1;
    private const uint WtdChoiceFile = 1;
    private const uint WtdStateActionVerify = 1;
    private const uint WtdStateActionClose = 2;
    private const uint WtdCacheOnlyUrlRetrieval = 0x1000;
    private const uint WtdRevocationCheckChainExcludeRoot = 0x80;

    /// <summary>True solo si la firma embebida es válida para el contenido actual y confiable.</summary>
    public static bool HasValidSignature(string filePath, bool checkRevocation)
    {
        if (!OperatingSystem.IsWindows() || !File.Exists(filePath))
            return false;

        IntPtr pathPtr = Marshal.StringToHGlobalUni(filePath);
        IntPtr fileInfoPtr = IntPtr.Zero;
        try
        {
            WinTrustFileInfo fileInfo = new()
            {
                CbStruct = (uint)Marshal.SizeOf<WinTrustFileInfo>(),
                PcwszFilePath = pathPtr
            };
            fileInfoPtr = Marshal.AllocHGlobal(Marshal.SizeOf<WinTrustFileInfo>());
            Marshal.StructureToPtr(fileInfo, fileInfoPtr, fDeleteOld: false);

            WinTrustData data = new()
            {
                CbStruct = (uint)Marshal.SizeOf<WinTrustData>(),
                DwUiChoice = WtdUiNone,
                FdwRevocationChecks = checkRevocation ? WtdRevokeWholeChain : WtdRevokeNone,
                DwUnionChoice = WtdChoiceFile,
                PFile = fileInfoPtr,
                DwStateAction = WtdStateActionVerify,
                DwProvFlags = checkRevocation ? WtdRevocationCheckChainExcludeRoot : WtdCacheOnlyUrlRetrieval
            };

            Guid action = GenericVerifyV2;
            int result = WinVerifyTrust(InvalidHandleValue, ref action, ref data);

            data.DwStateAction = WtdStateActionClose;
            _ = WinVerifyTrust(InvalidHandleValue, ref action, ref data);

            return result == 0;
        }
        finally
        {
            if (fileInfoPtr != IntPtr.Zero)
                Marshal.FreeHGlobal(fileInfoPtr);
            Marshal.FreeHGlobal(pathPtr);
        }
    }

    /// <summary>Huella (SHA-1, hex) del certificado firmante embebido, o null si no hay firma.</summary>
    public static string? SignerThumbprint(string filePath)
    {
        try
        {
#pragma warning disable SYSLIB0057 // X509CertificateLoader no expone la lectura de archivos firmados
            using X509Certificate2 signer = new(X509Certificate.CreateFromSignedFile(filePath));
#pragma warning restore SYSLIB0057
            return signer.Thumbprint;
        }
        catch (System.Security.Cryptography.CryptographicException)
        {
            return null;
        }
    }

    [LibraryImport("wintrust.dll")]
    private static partial int WinVerifyTrust(IntPtr hwnd, ref Guid pgActionId, ref WinTrustData pWvtData);

    [StructLayout(LayoutKind.Sequential)]
    private struct WinTrustFileInfo
    {
        public uint CbStruct;
        public IntPtr PcwszFilePath;
        public IntPtr HFile;
        public IntPtr PgKnownSubject;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct WinTrustData
    {
        public uint CbStruct;
        public IntPtr PPolicyCallbackData;
        public IntPtr PSipClientData;
        public uint DwUiChoice;
        public uint FdwRevocationChecks;
        public uint DwUnionChoice;
        public IntPtr PFile;
        public uint DwStateAction;
        public IntPtr HWvtStateData;
        public IntPtr PwszUrlReference;
        public uint DwProvFlags;
        public uint DwUiContext;
        public IntPtr PSignatureSettings;
    }
}
