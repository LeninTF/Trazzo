# Revisión de Security Hotspots — SonarCloud

**Proyecto:** `methodsync-trazzo-middleware`  
**Fecha de revisión:** 2026-06-10  
**Revisado por:** David Eduardo Peña Tinco

---

## Hotspot 1 — S4721: `Process.Start` en `AutoUpdateService`

### Ubicación
`Trazzo.Biometric.Agent/AutoUpdate/AutoUpdateService.cs` — método `ApplyUpdate`

### Descripción del riesgo
SonarCloud detecta cualquier llamada a `Process.Start` como un hotspot de seguridad porque, si el argumento proviene de datos externos, puede usarse para ejecutar comandos arbitrarios (Command Injection / CWE-78).

### Análisis del código

```csharp
private void ApplyUpdate(string msiPath)
{
    string tempDir = Path.GetTempPath();
    if (!msiPath.StartsWith(tempDir, StringComparison.OrdinalIgnoreCase)
        || !msiPath.EndsWith(".msi", StringComparison.OrdinalIgnoreCase))
    {
        _logger.LogError("Auto-Update: ruta del instalador fuera del directorio temporal. Instalación abortada.");
        return;
    }

    Process.Start(new ProcessStartInfo("msiexec.exe")
    {
        Arguments = $"/i \"{msiPath}\" /quiet /norestart",
        UseShellExecute = false,
        CreateNoWindow = true
    });
}
```

### Por qué es seguro

1. **El ejecutable es fijo**: `msiexec.exe` es un argumento literal, no proviene de ninguna fuente externa.
2. **La ruta del MSI es validada antes de usarse**:
   - Debe empezar con `Path.GetTempPath()` (directorio temporal del sistema).
   - Debe terminar en `.msi` (extensión literal, case-insensitive).
3. **La ruta del MSI es generada internamente**: se construye en `DownloadAndVerifyAsync` con `Path.GetTempPath()` y un `Guid` aleatorio (`TrazzoAgent-{version}-{guid}.msi`). Nunca proviene directamente del servidor.
4. **SHA-256 verificado**: antes de llegar a `ApplyUpdate`, el archivo MSI descargado es verificado criptográficamente contra el hash publicado en el manifiesto HTTPS.
5. **Manifiesto HTTPS-only**: la URL del manifiesto y la URL de descarga se validan para usar exclusivamente `https://` antes de cualquier petición HTTP.

### Mitigaciones adicionales disponibles (si se requieren)

- Firmar digitalmente los MSI con un certificado de código conocido y verificar la firma antes de ejecutar (`Get-AuthenticodeSignature`).
- Ejecutar `msiexec.exe` bajo una cuenta de servicio con privilegios mínimos.

### Veredicto para SonarCloud

**Marcar como: `Safe — Won't Fix`**

El `Process.Start` opera sobre un ejecutable fijo del sistema operativo (`msiexec.exe`) y una ruta validada por múltiples capas (directorio temporal, extensión, hash SHA-256, origen HTTPS). No existe vector de inyección de comandos.

---

## Hotspot 2 — S5332: URLs HTTP sin TLS

### Ubicación
- `Trazzo.Biometric.Agent/AutoUpdate/AutoUpdateService.cs` — `ExecuteAsync` y `CheckAndApplyUpdateAsync`
- `Trazzo.Biometric.Agent/Security/HybridCryptographyService.cs` — `BuildHttpFetcher`

### Descripción del riesgo
SonarCloud detecta URLs HTTP que podrían transmitir datos sensibles sin cifrado (CWE-319, Cleartext Transmission of Sensitive Information).

### Análisis del código

**AutoUpdateService — validación en `ExecuteAsync`:**
```csharp
if (!Uri.TryCreate(_manifestUrl, UriKind.Absolute, out Uri? manifestUri)
    || manifestUri.Scheme != Uri.UriSchemeHttps)
{
    _logger.LogError(
        "Auto-Update: la URL del manifiesto '{Url}' debe usar HTTPS. Auto-actualización deshabilitada por seguridad.",
        _manifestUrl);
    return;
}
```

**AutoUpdateService — validación de URL de descarga:**
```csharp
if (!Uri.TryCreate(manifest.DownloadUrl, UriKind.Absolute, out Uri? downloadUri)
    || downloadUri.Scheme != Uri.UriSchemeHttps)
{
    _logger.LogError(
        "Auto-Update: la URL de descarga '{Url}' no es HTTPS. Actualización abortada por seguridad.",
        manifest.DownloadUrl);
    return;
}
```

**HybridCryptographyService — validación en `BuildHttpFetcher`:**
```csharp
if (!Uri.TryCreate(url, UriKind.Absolute, out Uri? parsedUrl) || parsedUrl.Scheme != Uri.UriSchemeHttps)
{
    logger.LogError(
        "Security:BackendPublicKeyUrl='{Url}' debe usar HTTPS. La clave pública no se descargará.",
        url);
    return _ => Task.FromResult<string?>(null);
}
```

### Por qué es seguro

1. **Rechazo explícito de HTTP**: cualquier URL que no sea `https://` hace que el servicio aborte la operación con un log de error. No hay fallback a HTTP.
2. **Validación en dos puntos para Auto-Update**: tanto la URL del manifiesto (en startup del `BackgroundService`) como la URL de descarga (extraída del manifiesto) son validadas independientemente.
3. **Validación en startup para la clave RSA**: la URL de la clave pública se valida una sola vez al construir el fetcher. Si no es HTTPS, el fetcher devuelve `null` permanentemente.
4. **El WebSocket local** (`ws://localhost:9001`) no es un hotspot de seguridad porque opera exclusivamente en loopback; no hay datos sensibles en tránsito por la red.

### Veredicto para SonarCloud

**Marcar como: `Safe — Won't Fix`**

El código rechaza activamente cualquier URL HTTP. El hotspot fue la presencia de lógica de validación que compara contra `Uri.UriSchemeHttps`; SonarCloud detecta el patrón de comparación como indicio de posible uso de HTTP, pero en este caso la comparación es precisamente la guardia que bloquea HTTP.
