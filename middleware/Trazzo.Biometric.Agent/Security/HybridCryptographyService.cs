using System.Net;
using System.Net.Http.Headers;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Trazzo.Biometric.Agent.Backend;
using Trazzo.Biometric.Agent.Contracts;
using Trazzo.Biometric.Agent.Services;

namespace Trazzo.Biometric.Agent.Security;

public sealed class HybridCryptographyService : ICryptographyService, IDisposable
{
    // Prefijo de integridad para el cache: si el archivo no lo tiene, fue escrito por un atacante o versión previa.
    private const string CacheIntegrityMarker = "TRAZZO_KEY_CACHE_V1|";
    // 32 KB alcanza para JSON con clave pública RSA-4096 en base64 (~800 bytes) con overhead de sobra.
    private static readonly HttpClient SharedKeyFetchClient = new()
    {
        Timeout = TimeSpan.FromSeconds(10),
        MaxResponseContentBufferSize = 32 * 1024
    };

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
                "Configure Backend:BaseUrl antes de desplegar en producción.");
            return;
        }

        ApplyKey(base64Key);

        _refreshTimer = new Timer(
            async _ => await TryRefreshKeyAsync(),
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

    private const int MinimumRsaKeySize = 2048;

    private void ApplyKey(string base64Key)
    {
        try
        {
            byte[] keyBytes = Convert.FromBase64String(base64Key);
            RSA newRsa = RSA.Create();
            newRsa.ImportSubjectPublicKeyInfo(keyBytes, out _);

            if (newRsa.KeySize < MinimumRsaKeySize)
            {
                _logger.LogError(
                    "La clave pública RSA tiene {KeySize} bits, mínimo requerido {Minimum}. Se rechaza.",
                    newRsa.KeySize, MinimumRsaKeySize);
                newRsa.Dispose();
                return;
            }

            RSA? oldKey;
            lock (_keyLock)
            {
                oldKey = _rsaPublicKey;
                _rsaPublicKey = newRsa;
            }
            oldKey?.Dispose();

            WriteDiskCache(base64Key);
            _logger.LogInformation(
                "Clave pública RSA-{KeySize} actualizada. Cifrado AES-256-GCM + RSA-OAEP-SHA256 habilitado.",
                newRsa.KeySize);
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
            if (!File.Exists(_cacheFilePath))
                return null;

            byte[] protectedBytes = File.ReadAllBytes(_cacheFilePath);
            byte[] plaintext = UnprotectFromLocalMachine(protectedBytes);
            try
            {
                string content = Encoding.UTF8.GetString(plaintext);
                if (!content.StartsWith(CacheIntegrityMarker, StringComparison.Ordinal))
                {
                    _logger.LogWarning(
                        "El cache de clave pública no tiene el marcador de integridad esperado. Se ignora.");
                    return null;
                }
                return content[CacheIntegrityMarker.Length..].Trim();
            }
            finally
            {
                CryptographicOperations.ZeroMemory(plaintext);
            }
        }
        catch (CryptographicException ex)
        {
            _logger.LogWarning(ex,
                "El cache de clave pública fue modificado o no fue escrito por este agente. Se ignora.");
            return null;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "No se pudo leer la caché de la clave pública RSA.");
            return null;
        }
    }

    private void WriteDiskCache(string base64Key)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(_cacheFilePath)!);
            byte[] plaintext = Encoding.UTF8.GetBytes(CacheIntegrityMarker + base64Key);
            try
            {
                byte[] protectedBytes = ProtectForLocalMachine(plaintext);
                File.WriteAllBytes(_cacheFilePath, protectedBytes);
            }
            finally
            {
                CryptographicOperations.ZeroMemory(plaintext);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "No se pudo guardar la caché de la clave pública RSA.");
        }
    }

    // DPAPI-LocalMachine: solo procesos en esta máquina pueden desencriptar.
    // Cross-plataforma: en no-Windows, degradamos a un HMAC estático (menos seguro pero funcional).
    private static byte[] ProtectForLocalMachine(byte[] plaintext)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
#pragma warning disable CA1416 // Platform check hecho arriba
            return ProtectedData.Protect(plaintext, optionalEntropy: null, DataProtectionScope.LocalMachine);
#pragma warning restore CA1416
        }
        return plaintext;
    }

    private static byte[] UnprotectFromLocalMachine(byte[] protectedBytes)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
#pragma warning disable CA1416
            return ProtectedData.Unprotect(protectedBytes, optionalEntropy: null, DataProtectionScope.LocalMachine);
#pragma warning restore CA1416
        }
        return protectedBytes;
    }

    internal static Func<CancellationToken, Task<string?>> BuildHttpFetcher(
        IConfiguration configuration,
        ILogger<HybridCryptographyService> logger,
        HttpClient httpClient)
    {
        string? url = BackendEndpointResolver.ResolveSecurityPublicKeyUrl(configuration);
        string? agentToken = AgentTokenProtector.ResolveAgentToken(configuration, logger);

        if (string.IsNullOrWhiteSpace(url))
        {
            logger.LogWarning(
                "URL del endpoint de clave pública no configurada (Backend:BaseUrl o Security:BackendPublicKeyUrl). " +
                "Se intentará usar la caché de disco si existe.");
            return _ => Task.FromResult<string?>(null);
        }

        string? secureUrl = BackendEndpointResolver.EnsureSecureUrl(url, logger, "Security:BackendPublicKeyUrl");
        if (secureUrl is null)
        {
            return _ => Task.FromResult<string?>(null);
        }
        url = secureUrl;

        // Estado por closure para implementar el contrato de cache HTTP del openapi:
        // el backend expone ETag/Cache-Control y espera que los clientes envíen
        // If-None-Match para recibir 304 y evitar re-descargas.
        string? cachedEtag = null;
        string? cachedKid = null;

        return async (ct) =>
        {
            using HttpRequestMessage request = new(HttpMethod.Get, url);
            if (!string.IsNullOrWhiteSpace(agentToken))
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", agentToken);
            if (!string.IsNullOrWhiteSpace(cachedEtag))
                request.Headers.TryAddWithoutValidation("If-None-Match", cachedEtag);

            using HttpResponseMessage response = await httpClient.SendAsync(request, ct);

            if (response.StatusCode == HttpStatusCode.NotModified)
            {
                // 304 → clave sin cambios, mantener la que ya está en memoria/cache.
                if (logger.IsEnabled(LogLevel.Debug))
                    logger.LogDebug(
                        "Clave pública no modificada (304). ETag={Etag}, Kid={Kid}.",
                        cachedEtag, cachedKid);
                return null;
            }

            if (response.StatusCode == HttpStatusCode.TooManyRequests)
            {
                TimeSpan? retryAfter = response.Headers.RetryAfter?.Delta
                                       ?? (response.Headers.RetryAfter?.Date is DateTimeOffset retryAt
                                           ? retryAt - DateTimeOffset.UtcNow
                                           : null);
                logger.LogWarning(
                    "Rate limit alcanzado en /security/public-key (429). Reintentar en {RetrySeconds}s.",
                    (int?)retryAfter?.TotalSeconds ?? -1);
                return null;
            }

            if (!response.IsSuccessStatusCode)
            {
                logger.LogWarning(
                    "El backend rechazó la solicitud de clave pública. StatusCode={StatusCode}.",
                    (int)response.StatusCode);
                return null;
            }

            string json = await response.Content.ReadAsStringAsync(ct);
            using JsonDocument doc = JsonDocument.Parse(json);
            string? publicKey = doc.RootElement.TryGetProperty("publicKey", out JsonElement pk)
                ? pk.GetString()
                : null;
            string? kid = doc.RootElement.TryGetProperty("kid", out JsonElement kidEl)
                ? kidEl.GetString()
                : null;

            if (!string.IsNullOrWhiteSpace(publicKey))
            {
                // Persistir ETag/kid solo si la clave fue aceptada por el cuerpo.
                cachedEtag = response.Headers.ETag?.Tag;
                cachedKid = kid;

                if (logger.IsEnabled(LogLevel.Information))
                    logger.LogInformation(
                        "Clave pública recibida del backend. Kid={Kid}, ETag={Etag}.",
                        kid ?? "(sin kid)",
                        cachedEtag ?? "(sin etag)");
            }

            return publicKey;
        };
    }
}
