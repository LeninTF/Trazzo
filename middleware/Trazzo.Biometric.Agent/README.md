# Trazzo Biometric Agent

Middleware local de Windows que conecta el lector biométrico **ZKTeco ZK9500** con la plataforma SaaS Trazzo.

El agente corre como un Windows Service e instala sin necesitar .NET en la PC del colegio. Expone un WebSocket local en:

```text
ws://localhost:9001/
```

El frontend Angular se conecta a ese WebSocket, solicita operaciones biométricas y el agente habla con el lector por USB. Cuando captura una huella, extrae el template, lo cifra y lo envía al backend. Si no hay conexión, lo guarda en cola SQLite local y lo reintenta automáticamente.

---

## Estado del Middleware

### Implementado

| Módulo | Detalle |
|---|---|
| Windows Service | Instala con MSI, inicia automático, sin .NET en el cliente |
| WebSocket local | `ws://localhost:9001/`, heartbeat ping/pong cada 30 s |
| ZKTeco ZK9500 | Captura, identificación y enrolamiento (3 muestras + DBMerge) |
| Calidad de huella | Cobertura, contraste, centrado, tamaño mínimo de template |
| Cifrado híbrido | AES-256-GCM + RSA-2048-OAEP, clave pública desde el backend |
| Caché de clave RSA | `%PROGRAMDATA%\TrazzoAgent\public_key.cache` |
| Cola offline | SQLite en `%PROGRAMDATA%\TrazzoAgent\events.db` |
| Reenvío al backend | `POST /asistencia/sync` con `X-Tenant-ID` y `Authorization: Bearer` |
| Backoff exponencial | Reintento con jitter, hasta 5 minutos entre intentos |
| Rate limiting | Por cliente WebSocket, configurable (`Agent:RateLimitSeconds`) |
| CORS WebSocket | Restricción por origen (`Agent:AllowedOrigins`) |
| Multi-tenant | `X-Tenant-ID` en cada request al backend (`Agent:TenantId`) |
| Auto-updater | Descarga MSI desde manifiesto JSON, verifica SHA-256, instala silencioso |
| Recuperación de fallos | Servicio se reinicia solo en 10 s tras crash (configurado en el MSI) |
| Instalador visual | Bienvenida → Selección de ruta → Progreso → Finalizar |
| Imágenes del instalador | Banner 493×58 px y panel 493×312 px personalizados |
| CI/CD | GitHub Actions: 2 jobs — `build-test-analyze` (push y PR) + `package-msi` (solo push) |
| Tests | 218 pruebas xUnit (unitarias + integración), sin hardware real |
| Reporte de inicio | Log completo del estado de todos los módulos al arrancar |

### Pendiente para funcionamiento al 100%

| Qué | Responsable | Detalle |
|---|---|---|
| `GET /security/public-key` | Backend Spring Boot | Debe devolver `{ "publicKey": "string", "kid": "string" }` en PEM base64 |
| `POST /asistencia/sync` | Backend Spring Boot | Recibe array `[{ templateCifrado, llaveCifrada, timestampLocal, dispositivoId }]` con header `X-Tenant-ID` |
| Conexión WebSocket Angular | Frontend | Conectar a `ws://localhost:9001/` y manejar los mensajes JSON del protocolo |
| Configurar `appsettings.json` por tenant | DevOps / Trazzo | Rellenar `BackendUrl`, `TenantId`, `AgentToken`, `BackendPublicKeyUrl` antes de distribuir el MSI |
| Publicar en GitHub Releases | CI/CD | Agregar paso en el workflow para crear Release pública automáticamente en cada push a master. Sin esto el MSI solo es descargable por miembros del repo |
| Publicar manifiesto de auto-update | Infraestructura | Hostear el JSON de versiones en HTTPS (puede apuntar a GitHub Releases) para que el auto-updater funcione |
| Firma digital del MSI | Trazzo | Certificado de firma de código para evitar alertas de SmartScreen en Windows |
| Publicar en GitHub Releases | CI/CD | Falta el paso que sube el MSI con URL pública accesible para los técnicos de los colegios |

---

## Estructura del Proyecto

```text
middleware/
├── Directory.Build.props                # NuGetAuditSuppress GHSA-2m69-gcr7-jv3q (SQLite — mitigado con SourceGear.sqlite3)
├── Trazzo.Biometric.Agent/              # Servicio principal
│   ├── AutoUpdate/                      # Auto-updater silencioso
│   ├── Contracts/                       # Tipos de respuesta WebSocket
│   ├── Queue/                           # Cola SQLite + reenvío al backend
│   ├── Security/                        # Cifrado AES-256-GCM + RSA-2048
│   ├── Services/                        # Interfaces de servicios
│   ├── Utilities/                       # Análisis de calidad de huella
│   ├── WebSocket/                       # Servidor WebSocket local
│   ├── ZKTeco/                          # Integración SDK ZK9500
│   ├── scripts/
│   │   └── test-websocket.html          # Herramienta de prueba local
│   ├── Worker.cs                        # Orquestador principal
│   └── appsettings.json                 # Configuración
├── Trazzo.Biometric.Agent.Tests/        # 218 pruebas xUnit
└── Trazzo.Biometric.Agent.Installer/    # MSI con WiX Toolset v4
    ├── Resources/
    │   ├── banner.bmp                   # Franja superior del instalador (493×58 px)
    │   └── dialog.bmp                   # Panel de bienvenida/finalizar (493×312 px)
    ├── scripts/
    │   └── GenerateInstallerWxs.ps1     # Genera el .wxs desde el publish
    └── Product.wxs                      # Definición del instalador
```

---

## Requisitos de Desarrollo

- Windows 10 o superior (x64)
- .NET SDK 10
- Lector ZKTeco ZK9500 conectado por USB
- DLL del SDK ZKTeco en:

```text
Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll
```

La DLL no está en el repositorio porque pertenece al SDK propietario de ZKTeco. Para desarrollo local:

```powershell
Copy-Item `
  "C:\KZFingerSDK\ZKFingerSDK_Windows_Standard\ZKFinger Standard SDK 5.3.0.33\C#\lib\x64\libzkfpcsharp.dll" `
  ".\Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll" -Force
```

---

## Configuración Completa

Archivo: `Trazzo.Biometric.Agent\appsettings.json`

```json
{
  "Agent": {
    "WebSocketUrl": "http://localhost:9001/",
    "AllowedOrigins": [],
    "RateLimitSeconds": 5,
    "KeepAliveIntervalSeconds": 30,
    "TenantId": ""
  },
  "Biometric": {
    "CaptureTimeoutSeconds": 5,
    "CapturePollingIntervalMilliseconds": 80,
    "PostCaptureCooldownMilliseconds": 700,
    "RequireFingerLiftBeforeNextCapture": true,
    "TemplateBufferSize": 2048,
    "IncludeFingerprintImageInResponses": false,
    "Quality": {
      "MinimumTemplateSize": 400,
      "MinimumForegroundCoveragePercent": 18,
      "MaximumForegroundCoveragePercent": 75,
      "MinimumContrastScore": 25,
      "RequireCenteredFingerprint": true,
      "CenterTolerancePercent": 28,
      "ContrastThresholdOffset": 15
    }
  },
  "Enrollment": {
    "RequiredSamples": 3,
    "SampleTimeoutSeconds": 8,
    "RequireFingerLiftBetweenSamples": true
  },
  "Security": {
    "BackendPublicKeyUrl": ""
  },
  "Queue": {
    "DatabasePath": "",
    "BackendUrl": "",
    "AgentToken": "",
    "RetryIntervalSeconds": 30
  },
  "AutoUpdate": {
    "Enabled": false,
    "CheckIntervalMinutes": 60,
    "ManifestUrl": ""
  }
}
```

### Valores obligatorios en producción

| Clave | Descripción |
|---|---|
| `Agent:TenantId` | UUID del tenant (institución educativa). Se envía como `X-Tenant-ID` en cada request al backend |
| `Agent:AllowedOrigins` | Dominios permitidos para el WebSocket, ej. `["https://app.trazzo.pe"]` |
| `Security:BackendPublicKeyUrl` | URL del endpoint `GET /security/public-key` del backend |
| `Queue:BackendUrl` | URL del endpoint `POST /asistencia/sync` del backend |
| `Queue:AgentToken` | Token JWT del agente para autenticarse en el backend |
| `AutoUpdate:Enabled` | `true` para habilitar actualizaciones automáticas |
| `AutoUpdate:ManifestUrl` | URL HTTPS del manifiesto JSON de versiones |

### Manifiesto de auto-update

El auto-updater consume un JSON en `AutoUpdate:ManifestUrl` con este formato:

```json
{
  "version": "1.1.0",
  "downloadUrl": "https://releases.trazzo.pe/middleware/TrazzoAgent-1.1.0.msi",
  "sha256": "a3f1c2d4e5..."
}
```

Solo se aplica la actualización si la versión del manifiesto es mayor a la instalada y la URL es HTTPS. El SHA-256 se verifica antes de ejecutar el instalador.

---

## Compilar y Ejecutar Tests

```powershell
dotnet build .\Trazzo.Middleware.slnx -c Release
dotnet test
```

Los 218 tests no necesitan hardware. Usan implementaciones falsas (fakes) del SDK biométrico.

---

## Generar el MSI

```powershell
dotnet build .\Trazzo.Biometric.Agent.Installer\Trazzo.Biometric.Agent.Installer.wixproj -c Release
```

El MSI queda en:

```text
Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```

El MSI es **self-contained**: incluye el runtime de .NET 10. La PC del colegio no necesita tener .NET instalado.

---

## CI/CD con GitHub Actions

El pipeline `.github/workflows/middleware-ci.yml` se activa automáticamente en push (ramas `master` y `feature/**`) y en pull requests hacia `master`.

### Jobs

| Job | Trigger | Runner | Descripción |
|---|---|---|---|
| `build-test-analyze` | push y PR | Windows | Compila, ejecuta los 218 tests con cobertura (Coverlet) y envía análisis a SonarCloud. En push también publica el agente self-contained y sube los binarios como artefacto. |
| `package-msi` | solo push | Windows | Descarga los binarios del job anterior y genera el MSI con WiX v4 (`-p:SkipAgentPublish=true`). No re-publica el agente. |

El job `package-msi` depende de `build-test-analyze` y sólo corre en push, por lo que en pull requests sólo se ejecuta el primer job.

### Distribución a los colegios

Actualmente el MSI generado por CI queda como artefacto interno de GitHub Actions — solo accesible para miembros del repositorio. Para que el técnico del colegio pueda descargarlo con un simple link, falta agregar un paso al workflow que publique en **GitHub Releases**:

```
Push a master
  → CI compila y ejecuta 218 tests
  → CI genera MSI self-contained
  → CI publica GitHub Release con URL pública   ← pendiente de implementar
       ↓
Técnico del colegio:
  → Entra a github.com/<org>/<repo>/releases
  → Descarga Trazzo.Biometric.Agent.msi
  → Doble clic → instalador visual → instalar
       ↓
Auto-updater (una vez configurado el manifiesto):
  → El servicio se actualiza solo en futuras versiones
```

La PC del colegio solo necesita **Windows 10/11 x64**. El MSI ya incluye el runtime de .NET 10.

### Configurar el secret ZKFP_DLL_BASE64

```powershell
# Genera el valor Base64 de la DLL y lo copia al clipboard
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes("ruta\libzkfpcsharp.dll")
) | Set-Clipboard
```

Luego en GitHub: **Settings → Secrets and variables → Actions → New repository secret**
- **Name:** `ZKFP_DLL_BASE64`
- **Secret:** pegar el valor del clipboard

---

## Instalar el MSI

Doble clic en el MSI o desde PowerShell como administrador:

```powershell
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```

El instalador muestra:
1. Pantalla de bienvenida
2. Selección de carpeta de instalación (default: `C:\Program Files\Trazzo\BiometricAgent\`)
3. Confirmación
4. Progreso
5. Finalización

El servicio queda registrado y arrancado automáticamente:

```text
Nombre interno : TrazzoAgent
Display name   : Trazzo Biometric Agent
Inicio         : Automatic
Cuenta         : LocalSystem
```

En caso de crash, Windows reinicia el servicio automáticamente en 10 segundos.

Instalación silenciosa (para despliegue masivo con GPO o SCCM):

```powershell
msiexec /i Trazzo.Biometric.Agent.msi /quiet /norestart
```

---

## Verificar el Servicio

```powershell
# Estado del servicio
Get-Service -Name TrazzoAgent

# Puerto WebSocket activo
Get-NetTCPConnection -LocalPort 9001

# Health check HTTP
Invoke-RestMethod http://localhost:9001/health
```

---

## Probar Sin Instalar

```powershell
dotnet run --project .\Trazzo.Biometric.Agent\Trazzo.Biometric.Agent.csproj
```

Cuando aparezca `WebSocket escuchando en ws://localhost:9001/` el agente está listo.

Abre la herramienta de prueba en el navegador:

```text
Trazzo.Biometric.Agent\scripts\test-websocket.html
```

### Herramienta de prueba (`test-websocket.html`)

Página HTML autocontenida para probar el agente sin escribir código. No requiere servidor ni dependencias externas.

**Funcionalidades:**

| Botón | Acción |
|---|---|
| Conectar / Desconectar | Abre o cierra la conexión WebSocket. Al conectar se hace un health check automático |
| Estado del lector | Consulta si el ZK9500 está enchufado y el SDK inicializado |
| Capturar huella | Captura y cifra el template. El evento **no** se encola en SQLite |
| Identificar huella | Igual que capturar, pero el evento **sí** se encola para reenvío al backend |
| Enrolar huella | Captura 3 muestras consecutivas y genera template definitivo con DBMerge |
| Cancelar enrolamiento | Interrumpe un enrolamiento en curso |
| Limpiar | Limpia el log y resetea la UI |

**Tutorial integrado:** la primera vez que se abre la página aparece automáticamente un tutorial de 6 pasos. Se puede cerrar y volver a abrir desde el botón `? Tutorial` en la esquina superior derecha. La preferencia se guarda en `localStorage`.

**Sobre `templateBase64: null`:** es el comportamiento correcto cuando el cifrado está activo. El agente encontró una clave RSA en caché (`%PROGRAMDATA%\TrazzoAgent\public_key.cache`) y cifró el template. Los datos reales están en el campo `encryptedTemplate`. En desarrollo local sin backend, se puede eliminar el caché para ver el template en plano:

```powershell
Remove-Item "$env:ProgramData\TrazzoAgent" -Recurse -Force
```

---

## Protocolo WebSocket

Todos los mensajes son JSON. El cliente envía `{ "type": "..." }` y el agente responde.

### health.check

```json
{ "type": "health.check" }
```

```json
{
  "type": "health.check.result",
  "success": true,
  "message": "El agente biométrico de Trazzo está en ejecución."
}
```

### device.status

```json
{ "type": "device.status" }
```

```json
{
  "type": "device.status.result",
  "success": true,
  "isConnected": true,
  "deviceCount": 1,
  "message": "Lector biométrico conectado."
}
```

### fingerprint.capture

```json
{ "type": "fingerprint.capture" }
```

Captura simple. Devuelve el template cifrado y encola el evento offline.

### fingerprint.identify

```json
{ "type": "fingerprint.identify" }
```

Captura para asistencia. Encola el evento cifrado para reenvío al backend.

### fingerprint.enroll.start

```json
{ "type": "fingerprint.enroll.start" }
```

Enrolamiento de 3 muestras con DBMerge. Envía mensajes de progreso durante el proceso:

```json
{ "type": "fingerprint.enroll.progress", "sampleNumber": 1, "totalSamples": 3 }
```

### fingerprint.enroll.cancel

```json
{ "type": "fingerprint.enroll.cancel" }
```

### queue.status

```json
{ "type": "queue.status" }
```

```json
{
  "type": "queue.status.result",
  "success": true,
  "pendingCount": 3
}
```

### Respuesta de error

```json
{
  "type": "error",
  "success": false,
  "message": "Descripción del error."
}
```

---

## Dependencias de Seguridad

### SQLite — CVE-2025-6965 / GHSA-2m69-gcr7-jv3q

`Microsoft.Data.Sqlite` depende transitivamente de `SQLitePCLRaw.lib.e_sqlite3` 2.1.11, que embebe SQLite < 3.50.2. En esa versión el número de términos de un `aggregate` puede exceder las columnas disponibles, causando corrupción de memoria (vulnerabilidad alta).

**Mitigación activa:** el proyecto referencia `SourceGear.sqlite3` 3.50.4.5, que provee `e_sqlite3.dll` con SQLite 3.50.4 (≥ 3.50.2, parcheado). NuGet da prioridad a assets directos sobre transitivos, por lo que en runtime se carga la versión segura.

`SQLitePCLRaw.lib.e_sqlite3` no tiene versión parcheada disponible, por lo que sigue visible en el grafo transitivo y NuGet audit reporta `NU1903`. En `Directory.Build.props` se usa `<NuGetAuditSuppress>` apuntando exactamente al advisory `GHSA-2m69-gcr7-jv3q` para no ocultar futuros `NU1903` no relacionados. Las alertas Dependabot asociadas deben descartarse manualmente en GitHub como "riesgo tolerable — mitigado en runtime".

---

## Seguridad y Privacidad (Ley 29733)

El agente implementa **Privacidad por Diseño**:

- La imagen cruda de la huella **nunca sale de la RAM**. El SDK la convierte a template y se descarta.
- El template se cifra con AES-256-GCM antes de cualquier transmisión.
- La clave AES se cifra con RSA-2048-OAEP usando la clave pública del backend.
- Sin clave pública configurada, el agente arranca en modo sin cifrado y lo advierte en el log. No debe usarse en producción.

**Flujo de cifrado por captura:**

```
Huella física
  → SDK extrae template (binario, en RAM)
  → AES-256-GCM cifra el template (clave efímera por captura)
  → RSA-2048-OAEP cifra la clave AES (con clave pública del backend)
  → Se transmite: { templateCifrado, llaveCifrada, iv, tag }
  → Imagen cruda descartada
```

---

## Cola Offline

Los eventos se guardan en SQLite si el backend no está disponible:

```text
%PROGRAMDATA%\TrazzoAgent\events.db
```

El `EventForwarderService` reintenta el reenvío con backoff exponencial (base × 2^n, máximo 5 minutos, con ±10% de jitter). Después de 5 fallos consecutivos el evento queda como `failed`. Los eventos enviados o fallidos se limpian automáticamente después de 7 días.

---

## Recuperación ante Fallos

El instalador configura las acciones de recuperación del SCM de Windows:

| Fallo | Acción | Demora |
|---|---|---|
| 1er crash | Reinicio automático | 10 segundos |
| 2do crash | Reinicio automático | 10 segundos |
| 3er crash | Reinicio automático | 10 segundos |
| Reset del contador | Tras 24 h de funcionamiento estable | — |

---

## Errores Comunes

### WebSocket desconectado

El agente no está escuchando en `localhost:9001`.

```powershell
Get-Service -Name TrazzoAgent
Get-NetTCPConnection -LocalPort 9001
```

### DLL del SDK no encontrada

Falta `Native\x64\libzkfpcsharp.dll`. Copia la DLL oficial del SDK ZKTeco y vuelve a generar el MSI.

### No se encontró ningún lector biométrico

- El ZK9500 no está conectado por USB.
- Otro proceso de ZKTeco tiene el lector ocupado.

Cierra los procesos que puedan interferir:

```powershell
taskkill /F /IM ISSOnline_App.exe
taskkill /F /IM ISSOnline.exe
taskkill /F /IM iZHost.exe
taskkill /F /IM ZKOnlineProtect.exe
```

### Error MSI 1925 / 1603

El MSI requiere privilegios de administrador. Ejecuta PowerShell como administrador.

Para ver el log de instalación:

```powershell
msiexec /i Trazzo.Biometric.Agent.msi /l*v install.log
```

---

## Comandos Útiles

```powershell
# Compilar todo
dotnet build .\Trazzo.Middleware.slnx -c Release

# Ejecutar tests
dotnet test

# Generar MSI
dotnet build .\Trazzo.Biometric.Agent.Installer\Trazzo.Biometric.Agent.Installer.wixproj -c Release

# Instalar MSI
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi

# Instalar MSI silencioso
msiexec /i Trazzo.Biometric.Agent.msi /quiet /norestart

# Detener / iniciar servicio
Stop-Service -Name TrazzoAgent
Start-Service -Name TrazzoAgent

# Desinstalar
msiexec /x .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```
