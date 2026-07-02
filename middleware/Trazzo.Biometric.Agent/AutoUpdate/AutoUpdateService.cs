using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using System.Security.Cryptography;
using System.Text.Json;

namespace Trazzo.Biometric.Agent.AutoUpdate;

public sealed class AutoUpdateService : BackgroundService
{
    private static readonly HttpClient SharedHttpClient = new() { Timeout = TimeSpan.FromSeconds(30) };
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly bool _enabled;
    private readonly TimeSpan _checkInterval;
    private readonly string? _manifestUrl;
    private readonly HttpClient _httpClient;
    private readonly ILogger<AutoUpdateService> _logger;
    private readonly Func<TimeSpan, CancellationToken, Task> _delay;
    private readonly Func<Version> _getCurrentVersion;
    private readonly Action<string> _applyUpdate;
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
        string? updatesDirectory = null)
    {
        _logger = logger;
        _httpClient = httpClient;
        _enabled = configuration.GetValue("AutoUpdate:Enabled", false);
        int intervalMinutes = Math.Max(5, configuration.GetValue("AutoUpdate:CheckIntervalMinutes", 60));
        _checkInterval = TimeSpan.FromMinutes(intervalMinutes);
        _manifestUrl = configuration["AutoUpdate:ManifestUrl"];
        _delay = delay ?? Task.Delay;
        _getCurrentVersion = getCurrentVersion ?? GetCurrentVersion;
        _applyUpdate = applyUpdate ?? ApplyUpdate;
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
            using HttpResponseMessage response = await _httpClient.GetAsync(_manifestUrl, cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning(
                    "Auto-Update: no se pudo obtener el manifiesto. HTTP {Status}.",
                    (int)response.StatusCode);
                return null;
            }

            string json = await response.Content.ReadAsStringAsync(cancellationToken);
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
        string tempPath = Path.Combine(updatesDir, $"TrazzoAgent-{version}-{Guid.NewGuid():N}.msi");
        try
        {
            using HttpResponseMessage response = await _httpClient.GetAsync(downloadUri, cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning(
                    "Auto-Update: descarga fallida. HTTP {Status}.",
                    (int)response.StatusCode);
                return null;
            }

            await using (FileStream fs = new(tempPath, FileMode.Create, FileAccess.Write))
            {
                await response.Content.CopyToAsync(fs, cancellationToken);
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

        _logger.LogInformation("Auto-Update: instalador descargado y verificado correctamente.");
        return tempPath;
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
