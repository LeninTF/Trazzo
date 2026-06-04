using System.Diagnostics;
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
    private readonly ILogger<AutoUpdateService> _logger;

    public AutoUpdateService(IConfiguration configuration, ILogger<AutoUpdateService> logger)
    {
        _logger = logger;
        _enabled = configuration.GetValue("AutoUpdate:Enabled", false);
        int intervalMinutes = Math.Max(5, configuration.GetValue("AutoUpdate:CheckIntervalMinutes", 60));
        _checkInterval = TimeSpan.FromMinutes(intervalMinutes);
        _manifestUrl = configuration["AutoUpdate:ManifestUrl"];
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        if (!_enabled || string.IsNullOrWhiteSpace(_manifestUrl))
            return;

        // Espera inicial para no interferir con el arranque del servicio.
        try { await Task.Delay(TimeSpan.FromMinutes(1), stoppingToken); }
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

            try { await Task.Delay(_checkInterval, stoppingToken); }
            catch (OperationCanceledException) { return; }
        }
    }

    internal async Task CheckAndApplyUpdateAsync(CancellationToken cancellationToken)
    {
        UpdateManifest? manifest = await FetchManifestAsync(cancellationToken);
        if (manifest is null) return;

        if (!Version.TryParse(manifest.Version, out Version? remote) || remote is null)
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

        Version current = Assembly.GetEntryAssembly()?.GetName().Version ?? new Version(0, 0, 0, 0);

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

        ApplyUpdate(msiPath);
    }

    private async Task<UpdateManifest?> FetchManifestAsync(CancellationToken cancellationToken)
    {
        try
        {
            using HttpResponseMessage response = await SharedHttpClient.GetAsync(_manifestUrl, cancellationToken);
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

    private async Task<string?> DownloadAndVerifyAsync(
        string version, Uri downloadUri, string expectedSha256, CancellationToken cancellationToken)
    {
        string tempPath = Path.Combine(
            Path.GetTempPath(), $"TrazzoAgent-{version}-{Guid.NewGuid():N}.msi");
        try
        {
            using HttpResponseMessage response = await SharedHttpClient.GetAsync(downloadUri, cancellationToken);
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

        if (!string.IsNullOrWhiteSpace(expectedSha256) && !VerifySha256(tempPath, expectedSha256))
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

    private void ApplyUpdate(string msiPath)
    {
        try
        {
            _logger.LogInformation(
                "Auto-Update: lanzando instalador MSI. El servicio se reiniciará automáticamente...");

            Process.Start(new ProcessStartInfo("msiexec.exe")
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

    private static void TryDeleteFile(string path)
    {
        try { File.Delete(path); } catch { }
    }
}
