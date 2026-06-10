using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text.Json;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.Security;

public sealed class HybridCryptographyService : ICryptographyService, IDisposable
{
    private static readonly HttpClient SharedKeyFetchClient = new() { Timeout = TimeSpan.FromSeconds(10) };

    private readonly Func<CancellationToken, Task<string?>> _keyFetcher;
    private readonly ILogger<HybridCryptographyService> _logger;
    private readonly string _cacheFilePath;
    private readonly TimeSpan _refreshInterval;
    private RSA? _rsaPublicKey;
    private readonly object _keyLock = new();
    private Timer? _refreshTimer;
    private volatile bool _disposed;

    internal HybridCryptographyService(
        Func<CancellationToken, Task<string?>> keyFetcher,
        ILogger<HybridCryptographyService> logger,
        string? cacheFilePath = null,
        TimeSpan? refreshInterval = null)
    {
        _keyFetcher = keyFetcher;
        _logger = logger;
        _cacheFilePath = cacheFilePath ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
            "TrazzoAgent", "public_key.cache");
        _refreshInterval = refreshInterval ?? TimeSpan.FromHours(24);
    }

    public HybridCryptographyService(IConfiguration configuration, ILogger<HybridCryptographyService> logger)
        : this(BuildHttpFetcher(configuration, logger, SharedKeyFetchClient), logger)
    {
    }

    public bool IsConfigured { get { lock (_keyLock) return _rsaPublicKey is not null; } }

    public async Task InitializeAsync(CancellationToken cancellationToken)
    {
        string? base64Key = null;

        try
        {
            base64Key = await _keyFetcher(cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "No se pudo obtener la clave pública RSA del endpoint. Intentando caché de disco.");
        }

        base64Key ??= TryReadDiskCache();

        if (base64Key is null)
        {
            _logger.LogWarning(
                "No se encontró clave pública RSA (ni endpoint ni caché de disco). " +
                "Los templates biométricos se transmitirán sin cifrado AES-256/RSA-2048. " +
                "Configure Security:BackendPublicKeyUrl antes de desplegar en producción.");
            return;
        }

        ApplyKey(base64Key);

        _refreshTimer = new Timer(
            _ => _ = TryRefreshKeyAsync(),
            null,
            _refreshInterval,
            _refreshInterval);
    }

    public EncryptedPayload? TryEncryptTemplate(byte[] template, int templateSize)
    {
        RSA? rsa;
        lock (_keyLock) rsa = _rsaPublicKey;
        if (rsa is null) return null;

        ReadOnlySpan<byte> plaintext = template.AsSpan(0, templateSize);

        byte[] aesKey = RandomNumberGenerator.GetBytes(32);
        byte[] iv = RandomNumberGenerator.GetBytes(12);
        byte[] tag = new byte[16];
        byte[] ciphertext = new byte[plaintext.Length];

        using (AesGcm aes = new(aesKey, tagSizeInBytes: 16))
        {
            aes.Encrypt(iv, plaintext, ciphertext, tag);
        }

        byte[] encryptedAesKey = rsa.Encrypt(aesKey, RSAEncryptionPadding.OaepSHA256);

        return new EncryptedPayload(
            Convert.ToBase64String(ciphertext),
            Convert.ToBase64String(encryptedAesKey),
            Convert.ToBase64String(iv),
            Convert.ToBase64String(tag));
    }

    public void Dispose()
    {
        _disposed = true;
        _refreshTimer?.Change(Timeout.Infinite, Timeout.Infinite);
        _refreshTimer?.Dispose();
        _refreshTimer = null;
        lock (_keyLock)
        {
            _rsaPublicKey?.Dispose();
            _rsaPublicKey = null;
        }
    }

    internal async Task TryRefreshKeyAsync()
    {
        if (_disposed) return;
        try
        {
            string? base64Key = await _keyFetcher(CancellationToken.None);
            if (base64Key is not null && !_disposed)
                ApplyKey(base64Key);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Error al refrescar la clave pública RSA. Se mantiene la clave anterior.");
        }
    }

    private void ApplyKey(string base64Key)
    {
        try
        {
            byte[] keyBytes = Convert.FromBase64String(base64Key);
            RSA newRsa = RSA.Create();
            newRsa.ImportSubjectPublicKeyInfo(keyBytes, out _);

            RSA? oldKey;
            lock (_keyLock)
            {
                oldKey = _rsaPublicKey;
                _rsaPublicKey = newRsa;
            }
            oldKey?.Dispose();

            WriteDiskCache(base64Key);
            _logger.LogInformation("Clave pública RSA-2048 actualizada. Cifrado AES-256-GCM + RSA-2048-OAEP habilitado.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al aplicar la clave pública RSA. Cifrado no actualizado.");
        }
    }

    private string? TryReadDiskCache()
    {
        try
        {
            if (File.Exists(_cacheFilePath))
                return File.ReadAllText(_cacheFilePath).Trim();
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "No se pudo leer la caché de la clave pública RSA.");
        }
        return null;
    }

    private void WriteDiskCache(string base64Key)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(_cacheFilePath)!);
            File.WriteAllText(_cacheFilePath, base64Key);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "No se pudo guardar la caché de la clave pública RSA.");
        }
    }

    internal static Func<CancellationToken, Task<string?>> BuildHttpFetcher(
        IConfiguration configuration,
        ILogger<HybridCryptographyService> logger,
        HttpClient httpClient)
    {
        string? url = configuration["Security:BackendPublicKeyUrl"];
        string? agentToken = configuration["Queue:AgentToken"];

        if (string.IsNullOrWhiteSpace(url))
        {
            logger.LogWarning(
                "URL del endpoint de clave pública no configurada (Security:BackendPublicKeyUrl). " +
                "Se intentará usar la caché de disco si existe.");
            return _ => Task.FromResult<string?>(null);
        }

        if (!Uri.TryCreate(url, UriKind.Absolute, out Uri? parsedUrl) || parsedUrl.Scheme != Uri.UriSchemeHttps)
        {
            logger.LogError(
                "Security:BackendPublicKeyUrl='{Url}' debe usar HTTPS. La clave pública no se descargará.",
                url);
            return _ => Task.FromResult<string?>(null);
        }

        return async (ct) =>
        {
            using HttpRequestMessage request = new(HttpMethod.Get, url);
            if (!string.IsNullOrWhiteSpace(agentToken))
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", agentToken);

            using HttpResponseMessage response = await httpClient.SendAsync(request, ct);
            string json = await response.Content.ReadAsStringAsync(ct);
            using JsonDocument doc = JsonDocument.Parse(json);
            return doc.RootElement.GetProperty("publicKey").GetString();
        };
    }
}
