using System.Security.Cryptography.X509Certificates;
using Microsoft.Extensions.Logging.Abstractions;
using Trazzo.Biometric.Agent.AutoUpdate;

namespace Trazzo.Biometric.Agent.Tests;

// Usa dotnet.exe como binario con firma Authenticode embebida (Microsoft): existe en cualquier
// máquina que ejecuta estos tests, incluido el runner de CI.
public class AuthenticodeVerifierTests : IDisposable
{
    private readonly string _workDir = Path.Combine(Path.GetTempPath(), $"authenticode-{Guid.NewGuid():N}");

    public AuthenticodeVerifierTests() => Directory.CreateDirectory(_workDir);

    public void Dispose() => Directory.Delete(_workDir, recursive: true);

    private static string SignedBinary()
    {
        string path = Path.Combine(Path.GetDirectoryName(Environment.ProcessPath)!, "dotnet.exe");
        return File.Exists(path) ? path : Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "dotnet", "dotnet.exe");
    }

    [Fact]
    public void HasValidSignature_CuandoArchivoNoEstaFirmado_DevuelveFalse()
    {
        string unsigned = Path.Combine(_workDir, "unsigned.msi");
        File.WriteAllBytes(unsigned, [0x4d, 0x5a, 0x90, 0x00, 1, 2, 3, 4]);

        Assert.False(AuthenticodeVerifier.HasValidSignature(unsigned, checkRevocation: false));
    }

    [Fact]
    public void HasValidSignature_CuandoFirmaEsValida_DevuelveTrue()
    {
        string copy = Path.Combine(_workDir, "signed.exe");
        File.Copy(SignedBinary(), copy);

        Assert.True(AuthenticodeVerifier.HasValidSignature(copy, checkRevocation: false));
    }

    [Fact]
    public void HasValidSignature_CuandoContenidoFueAlteradoTrasFirmar_DevuelveFalse()
    {
        // El certificado embebido sigue intacto (CreateFromSignedFile lo lee y su cadena es válida),
        // pero el digest ya no corresponde al contenido: solo WinVerifyTrust lo detecta.
        string tampered = Path.Combine(_workDir, "tampered.exe");
        File.Copy(SignedBinary(), tampered);
        byte[] bytes = File.ReadAllBytes(tampered);
        bytes[bytes.Length / 3] ^= 0xFF;
        File.WriteAllBytes(tampered, bytes);

#pragma warning disable SYSLIB0057
        using X509Certificate2 embedded = new(X509Certificate.CreateFromSignedFile(tampered));
#pragma warning restore SYSLIB0057
        Assert.Contains("Microsoft", embedded.Subject);
        Assert.False(AuthenticodeVerifier.HasValidSignature(tampered, checkRevocation: false));
    }

    // CN del firmante leído con otra API (GetNameInfo), no con ExtractCommonName del código bajo prueba.
    private static string SignerCommonName()
    {
#pragma warning disable SYSLIB0057
        using X509Certificate2 signer = new(X509Certificate.CreateFromSignedFile(SignedBinary()));
#pragma warning restore SYSLIB0057
        return signer.GetNameInfo(X509NameType.SimpleName, forIssuer: false);
    }

    [Fact]
    public void IsTrustedInstaller_CuandoPublisherEsElEsperado_LoAcepta()
    {
        Assert.True(AutoUpdateService.IsTrustedInstaller(
            SignedBinary(), SignerCommonName(), trustedThumbprints: [], checkRevocation: false, NullLogger.Instance));
    }

    [Fact]
    public void IsTrustedInstaller_CuandoPublisherEsOtro_LoRechaza()
    {
        Assert.False(AutoUpdateService.IsTrustedInstaller(
            SignedBinary(), "Trazzo", trustedThumbprints: [], checkRevocation: false, NullLogger.Instance));
    }

    [Fact]
    public void IsTrustedInstaller_ConThumbprintsConfigurados_SoloAceptaElFirmantePinneado()
    {
#pragma warning disable SYSLIB0057
        using X509Certificate2 signer = new(X509Certificate.CreateFromSignedFile(SignedBinary()));
#pragma warning restore SYSLIB0057

        Assert.True(AutoUpdateService.IsTrustedInstaller(
            SignedBinary(), SignerCommonName(), [signer.Thumbprint.ToLowerInvariant()], false, NullLogger.Instance));
        Assert.False(AutoUpdateService.IsTrustedInstaller(
            SignedBinary(), SignerCommonName(), ["0000000000000000000000000000000000000000"], false, NullLogger.Instance));
    }

    [Fact]
    public void IsTrustedInstaller_CuandoInstaladorFueAlterado_LoRechaza()
    {
        string tampered = Path.Combine(_workDir, "tampered-installer.exe");
        File.Copy(SignedBinary(), tampered);
        byte[] bytes = File.ReadAllBytes(tampered);
        bytes[bytes.Length / 3] ^= 0xFF;
        File.WriteAllBytes(tampered, bytes);

        Assert.False(AutoUpdateService.IsTrustedInstaller(
            tampered, SignerCommonName(), trustedThumbprints: [], checkRevocation: false, NullLogger.Instance));
    }

    [Fact]
    public void SignerThumbprint_DevuelveLaHuellaDelCertificadoFirmante()
    {
#pragma warning disable SYSLIB0057
        using X509Certificate2 signer = new(X509Certificate.CreateFromSignedFile(SignedBinary()));
#pragma warning restore SYSLIB0057

        Assert.Equal(signer.Thumbprint, AuthenticodeVerifier.SignerThumbprint(SignedBinary()), ignoreCase: true);
    }
}
