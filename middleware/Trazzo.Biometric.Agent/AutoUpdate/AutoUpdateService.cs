using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Text.Json;

namespace Trazzo.Biometric.Agent.AutoUpdate;

public sealed class AutoUpdateService : BackgroundService
{
    private static readonly HttpClient SharedHttpClient = new() { Timeout = TimeSpan.FromSeconds(30) };
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    // Límite para el manifest (JSON con version + url + sha).
    private const int MaxManifestBytes = 16 * 1024;
    // Límite duro para el MSI (200 MB). Configurable via AutoUpdate:MaxMsiBytes.
    private const long DefaultMaxMsiBytes = 200L * 1024 * 1024;

    private readonly bool _enabled;
    private readonly TimeSpan _checkInterval;
    private readonly string? _manifestUrl;
    private readonly string _expectedPublisherCn;
    private readonly long _maxMsiBytes;
    private readonly HttpClient _httpClient;
    private readonly ILogger<AutoUpdateService> _logger;
    private readonly Func<TimeSpan, CancellationToken, Task> _delay;
    private readonly Func<Version> _getCurrentVersion;
    private readonly Action<string> _applyUpdate;
    private readonly Func<string, string, bool> _verifyAuthenticode;
    private readonly string _updatesDirectory;

    public AutoUpdateService(IConfiguration configuration, ILogger<AutoUpdateService> logger)
        : this(configuration, logger, SharedHttpClient)
    {
    }

    internal AutoUpdateService(
        IConfiguration configuration,
        ILogger<AutoUpdateService> logger,
        HttpClient httpClient,
        Func<TimeSpan, CancellationToken, Task>? delay = null,
        Func<Version>? getCurrentVersion = null,
        Action<string>? applyUpdate = null,
        string? updatesDirectory = null,
        Func<string, string, bool>? verifyAuthenticode = null)
    {
        _logger = logger;
        _httpClient = httpClient;
        _enabled = configuration.GetValue("AutoUpdate:Enabled", false);
        int intervalMinutes = Math.Max(5, configuration.GetValue("AutoUpdate:CheckIntervalMinutes", 60));
        _checkInterval = TimeSpan.FromMinutes(intervalMinutes);
        _manifestUrl = configuration["AutoUpdate:ManifestUrl"];
        _expectedPublisherCn = configuration["AutoUpdate:PublisherCommonName"] ?? "Trazzo";
        _maxMsiBytes = configuration.GetValue("AutoUpdate:MaxMsiBytes", DefaultMaxMsiBytes);
        _delay = delay ?? Task.Delay;
        _getCurrentVersion = getCurrentVersion ?? GetCurrentVersion;
        _applyUpdate = applyUpdate ?? ApplyUpdate;
        _verifyAuthenticode = verifyAuthenticode ?? VerifyAuthenticodePublisher;
        _updatesDirectory = updatesDirectory ?? GetUpdatesDirectory();
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_enabled || string.IsNullOrWhiteSpace(_manifestUrl))
            return;

        if (!Uri.TryCreate(_manifestUrl, UriKind.Absolute, out Uri? manifestUri)
            || manifestUri.Scheme != Uri.UriSchemeHttps)
        {
            _logger.LogError(
                "Auto-Update: la URL del manifiesto '{Url}' debe usar HTTPS. Auto-actualización deshabilitada por seguridad.",
                _manifestUrl);
            return;
        }

        try { await _delay(TimeSpan.FromMinutes(1), stoppingToken); }
        catch (OperationCanceledException) { return; }

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await CheckAndApplyUpdateAsync(stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                return;
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Auto-Update: error inesperado al verificar actualizaciones.");
            }

            try { await _delay(_checkInterval, stoppingToken); }
            catch (OperationCanceledException) { return; }
        }
    }

    internal async Task CheckAndApplyUpdateAsync(CancellationToken cancellationToken)
    {
        UpdateManifest? manifest = await FetchManifestAsync(cancellationToken);
        if (manifest is null) return;

        if (!Version.TryParse(manifest.Version, out Version? remote))
        {
            _logger.LogWarning("Auto-Update: el manifiesto contiene una versión inválida: '{Version}'.", manifest.Version);
            return;
        }

        if (!Uri.TryCreate(manifest.DownloadUrl, UriKind.Absolute, out Uri? downloadUri)
            || downloadUri.Scheme != Uri.UriSchemeHttps)
        {
            _logger.LogError(
                "Auto-Update: la URL de descarga '{Url}' no es HTTPS. Actualización abortada por seguridad.",
                manifest.DownloadUrl);
            return;
        }

        Version current = _getCurrentVersion();

        if (remote <= current)
        {
            _logger.LogDebug("Auto-Update: versión actual {Current} está al día.", current);
            return;
        }

        _logger.LogInformation(
            "Auto-Update: nueva versión {Remote} disponible (actual: {Current}). Descargando instalador...",
            remote, current);

        string? msiPath = await DownloadAndVerifyAsync(manifest.Version, downloadUri, manifest.Sha256, cancellationToken);
        if (msiPath is null) return;

        _applyUpdate(msiPath);
    }

    private async Task<UpdateManifest?> FetchManifestAsync(CancellationToken cancellationToken)
    {
        try
        {
            using HttpResponseMessage response = await _httpClient.GetAsync(
                _manifestUrl, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning(
                    "Auto-Update: no se pudo obtener el manifiesto. HTTP {Status}.",
                    (int)response.StatusCode);
                return null;
            }

            long? contentLength = response.Content.Headers.ContentLength;
            if (contentLength is > MaxManifestBytes)
            {
                _logger.LogWarning(
                    "Auto-Update: manifiesto excede el máximo permitido ({ActualBytes} > {MaxBytes} bytes).",
                    contentLength.Value, MaxManifestBytes);
                return null;
            }

            await using Stream body = await response.Content.ReadAsStreamAsync(cancellationToken);
            using MemoryStream buffer = new(capacity: 4096);
            byte[] readBuf = new byte[4096];
            int read;
            while ((read = await body.ReadAsync(readBuf, cancellationToken)) > 0)
            {
                if (buffer.Length + read > MaxManifestBytes)
                {
                    _logger.LogWarning(
                        "Auto-Update: manifiesto excede el máximo permitido ({MaxBytes} bytes) durante lectura.",
                        MaxManifestBytes);
                    return null;
                }
                buffer.Write(readBuf, 0, read);
            }

            string json = System.Text.Encoding.UTF8.GetString(buffer.GetBuffer(), 0, (int)buffer.Length);
            return JsonSerializer.Deserialize<UpdateManifest>(json, JsonOptions);
        }
        catch (Exception ex) when (!cancellationToken.IsCancellationRequested)
        {
            _logger.LogWarning(ex, "Auto-Update: error al obtener el manifiesto desde {Url}.", _manifestUrl);
            return null;
        }
    }

    private static string GetUpdatesDirectory()
        => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "TrazzoAgent", "updates");

    private async Task<string?> DownloadAndVerifyAsync(
        string version, Uri downloadUri, string expectedSha256, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(expectedSha256))
        {
            _logger.LogError(
                "Auto-Update: el manifiesto no incluye SHA-256. Actualizacion abortada por seguridad.");
            return null;
        }

        string updatesDir = _updatesDirectory;
        Directory.CreateDirectory(updatesDir);
        CleanupOldMsiFiles(updatesDir);
        string tempPath = Path.Combine(updatesDir, $"TrazzoAgent-{version}-{Guid.NewGuid():N}.msi");
        try
        {
            using HttpResponseMessage response = await _httpClient.GetAsync(
                downloadUri, HttpCompletionOption.ResponseHeadersRead, cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning(
                    "Auto-Update: descarga fallida. HTTP {Status}.",
                    (int)response.StatusCode);
                return null;
            }

            long? contentLength = response.Content.Headers.ContentLength;
            if (contentLength is null)
            {
                _logger.LogWarning(
                    "Auto-Update: la respuesta no incluye Content-Length. Se requiere para validar el tamaño del MSI.");
                return null;
            }
            if (contentLength.Value > _maxMsiBytes)
            {
                _logger.LogWarning(
                    "Auto-Update: MSI excede el máximo permitido ({ActualBytes} > {MaxBytes} bytes).",
                    contentLength.Value, _maxMsiBytes);
                return null;
            }

            await using Stream body = await response.Content.ReadAsStreamAsync(cancellationToken);
            await using FileStream fs = new(tempPath, FileMode.Create, FileAccess.Write);
            byte[] copyBuffer = new byte[81920];
            long total = 0;
            int read;
            while ((read = await body.ReadAsync(copyBuffer, cancellationToken)) > 0)
            {
                total += read;
                if (total > _maxMsiBytes)
                {
                    _logger.LogWarning(
                        "Auto-Update: MSI excede el máximo permitido ({MaxBytes} bytes) durante lectura.",
                        _maxMsiBytes);
                    TryDeleteFile(tempPath);
                    return null;
                }
                await fs.WriteAsync(copyBuffer.AsMemory(0, read), cancellationToken);
            }
        }
        catch (Exception ex) when (!cancellationToken.IsCancellationRequested)
        {
            _logger.LogWarning(ex, "Auto-Update: error al descargar el instalador.");
            TryDeleteFile(tempPath);
            return null;
        }

        if (!VerifySha256(tempPath, expectedSha256))
        {
            _logger.LogError(
                "Auto-Update: la verificación SHA-256 del instalador falló. Instalación abortada.");
            TryDeleteFile(tempPath);
            return null;
        }

        if (!_verifyAuthenticode(tempPath, _expectedPublisherCn))
        {
            _logger.LogError(
                "Auto-Update: la firma Authenticode no coincide con el publisher esperado '{PublisherCn}'. Instalación abortada.",
                _expectedPublisherCn);
            TryDeleteFile(tempPath);
            return null;
        }

        _logger.LogInformation("Auto-Update: instalador descargado y verificado correctamente (SHA-256 + Authenticode).");
        return tempPath;
    }

    [ExcludeFromCodeCoverage(Justification = "Requires signed MSI to test end-to-end.")]
    private bool VerifyAuthenticodePublisher(string filePath, string expectedPublisherCn)
    {
        try
        {
            // X509Certificate.CreateFromSignedFile lee el certificado embebido del Authenticode.
            // No es suficiente por sí solo (no valida cadena/CRL), pero comprobamos que el CN coincide.
            // El warning SYSLIB0057 sugiere X509CertificateLoader, que aún no expone equivalente para signed files.
#pragma warning disable SYSLIB0057
            X509Certificate certificate = X509Certificate.CreateFromSignedFile(filePath);
            using X509Certificate2 cert2 = new(certificate);
#pragma warning restore SYSLIB0057

            string subjectCn = ExtractCommonName(cert2.Subject);
            if (!string.Equals(subjectCn, expectedPublisherCn, StringComparison.OrdinalIgnoreCase))
            {
                _logger.LogWarning(
                    "Auto-Update: Publisher CN='{ActualCn}' no coincide con el esperado '{ExpectedCn}'.",
                    subjectCn, expectedPublisherCn);
                return false;
            }

            // Validación de cadena y confianza contra el trust store del sistema.
            using X509Chain chain = new()
            {
                ChainPolicy =
                {
                    RevocationMode = X509RevocationMode.Online,
                    RevocationFlag = X509RevocationFlag.ExcludeRoot,
                    VerificationFlags = X509VerificationFlags.NoFlag
                }
            };
            bool chainOk = chain.Build(cert2);
            if (!chainOk)
            {
                foreach (X509ChainStatus status in chain.ChainStatus)
                {
                    _logger.LogWarning(
                        "Auto-Update: cadena de certificados inválida. {Status}: {Info}",
                        status.Status, status.StatusInformation);
                }
                return false;
            }

            return true;
        }
        catch (CryptographicException ex)
        {
            _logger.LogError(ex, "Auto-Update: el archivo no está firmado o la firma es inválida.");
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Auto-Update: error al validar la firma Authenticode.");
            return false;
        }
    }

    private static string ExtractCommonName(string subject)
    {
        // Subject viene como "CN=Trazzo, O=..., L=..."
        foreach (string part in subject.Split(','))
        {
            string trimmed = part.Trim();
            if (trimmed.StartsWith("CN=", StringComparison.OrdinalIgnoreCase))
                return trimmed[3..];
        }
        return subject;
    }

    private static bool VerifySha256(string filePath, string expectedHex)
    {
        using FileStream fs = File.OpenRead(filePath);
        byte[] hash = SHA256.HashData(fs);
        string actual = Convert.ToHexStringLower(hash);
        return string.Equals(actual, expectedHex.ToLowerInvariant(), StringComparison.Ordinal);
    }

    private static Version GetCurrentVersion()
        => Assembly.GetEntryAssembly()?.GetName().Version ?? new Version(0, 0, 0, 0);

    [ExcludeFromCodeCoverage]
    private void ApplyUpdate(string msiPath)
    {
        string updatesDir = GetUpdatesDirectory();
        if (!msiPath.StartsWith(updatesDir, StringComparison.OrdinalIgnoreCase)
            || !msiPath.EndsWith(".msi", StringComparison.OrdinalIgnoreCase))
        {
            _logger.LogError("Auto-Update: ruta del instalador fuera del directorio de actualizaciones. Instalación abortada.");
            return;
        }

        string msiexecPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.System),
            "msiexec.exe");

        try
        {
            _logger.LogInformation(
                "Auto-Update: lanzando instalador MSI. El servicio se reiniciará automáticamente...");

            Process.Start(new ProcessStartInfo(msiexecPath)
            {
                Arguments = $"/i \"{msiPath}\" /quiet /norestart",
                UseShellExecute = false,
                CreateNoWindow = true
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Auto-Update: error al ejecutar el instalador MSI.");
            TryDeleteFile(msiPath);
        }
    }

    // Elimina .msi antiguos del directorio de updates para no acumularlos en disco.
    // Mantiene los últimos 3 por si se necesita rollback manual.
    private void CleanupOldMsiFiles(string updatesDir)
    {
        try
        {
            var oldFiles = new DirectoryInfo(updatesDir)
                .GetFiles("TrazzoAgent-*.msi")
                .OrderByDescending(f => f.LastWriteTimeUtc)
                .Skip(3)
                .ToArray();

            foreach (FileInfo file in oldFiles)
            {
                try { file.Delete(); }
                catch (Exception ex)
                {
                    _logger.LogDebug(ex, "Auto-Update: no se pudo borrar MSI antiguo {File}.", file.Name);
                }
            }

            if (oldFiles.Length > 0)
                _logger.LogInformation("Auto-Update: {Count} MSI antiguo(s) eliminado(s) del directorio de updates.", oldFiles.Length);
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Auto-Update: error no crítico en cleanup de updates directory.");
        }
    }

    [ExcludeFromCodeCoverage]
    private void TryDeleteFile(string path)
    {
        try { File.Delete(path); }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Auto-Update: no se pudo eliminar el archivo temporal.");
        }
    }
}
