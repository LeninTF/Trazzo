# Trazzo Biometric Agent

Middleware local de Windows que conecta el lector biométrico **ZKTeco ZK9500** con la plataforma SaaS Trazzo.

El agente corre como un Windows Service e instala sin necesitar .NET en la PC del colegio. Expone un WebSocket local en:

```text
ws://localhost:9001/
```

El frontend Angular se conecta a ese WebSocket, solicita operaciones biométricas y el agente habla con el lector por USB. Para asistencia, el agente captura la huella, cifra el template y llama a `POST /asistencia/marcar`; si no puede completar esa marcación síncrona, guarda el evento en SQLite y lo reintenta por `POST /asistencia/sync`.

---

## Estado del Middleware

### Implementado

| Módulo | Detalle |
|---|---|
| Windows Service | Instala con MSI, inicia automático, sin .NET en el cliente |
| WebSocket local | `ws://localhost:9001/`, heartbeat ping/pong cada 30 s |
| ZKTeco ZK9500 | Captura, identificación y enrolamiento (3 muestras + DBMerge) |
| Calidad de huella | Cobertura, contraste, centrado, tamaño mínimo de template |
| Cifrado híbrido | AES-256-GCM + RSA-2048-OAEP (validación de KeySize ≥ 2048), clave pública desde el backend |
| Caché de clave RSA | `%PROGRAMDATA%\TrazzoAgent\public_key.cache` cifrado con DPAPI-LocalMachine + marcador de integridad |
| Cola offline | SQLite (WAL + connection pooling) en `%PROGRAMDATA%\TrazzoAgent\events.db` |
| Marcación síncrona | `POST /asistencia/marcar` con `X-Tenant-ID` y `Authorization: Bearer` |
| Reenvío offline al backend | `POST /asistencia/sync` con `X-Tenant-ID` y `Authorization: Bearer` |
| Backoff exponencial | Reintento con jitter, hasta 5 minutos entre intentos |
| Operaciones repetidas | Captura, identificación y enrolamiento pueden repetirse al terminar la operación anterior |
| CORS WebSocket | Restricción por origen (`Agent:AllowedOrigins`) |
| Rate-limit WebSocket | Cap por cliente: 256 KB por mensaje, 8 operaciones concurrentes máx. (configurable) |
| Multi-tenant | `X-Tenant-ID` en cada request al backend (`Agent:TenantId`) |
| HTTPS obligatorio | Todos los endpoints de backend deben ser HTTPS (loopback permitido para desarrollo) |
| Guard plaintext template | Templates biométricos nunca viajan sin cifrar salvo `TRAZZO_ALLOW_PLAINTEXT_TEMPLATES=true` |
| Auto-updater | Descarga MSI desde manifiesto JSON, verifica SHA-256 + firma Authenticode + publisher CN, cap de tamaño (200 MB), instala silencioso |
| Cleanup de updates | Mantiene solo los últimos 3 MSIs descargados para evitar acumulación en disco |
| Recuperación de fallos | Servicio se reinicia solo en 10 s tras crash (configurado en el MSI) |
| Instalador visual | Bienvenida → Selección de ruta → Progreso → Finalizar |
| Imágenes del instalador | Banner 493×58 px y panel 493×312 px personalizados |
| CI/CD | GitHub Actions: 2 jobs — `build-test-analyze` (push y PR) + `package-msi` (solo push) |
| Tests | 345 pruebas xUnit sin necesidad de hardware biométrico |
| Reporte de inicio | Log completo del estado de todos los módulos al arrancar |

### Pendiente para funcionamiento al 100%

| Qué | Responsable | Detalle |
|---|---|---|
| `GET /security/public-key` | Backend Spring Boot | Debe devolver `{ "publicKey": "string", "kid": "string" }` en PEM base64 |
| `POST /asistencia/marcar` | Backend Spring Boot | Recibe objeto `{ event_type: "identify", encrypted_template_base64, encrypted_aes_key_base64, iv_base64, tag_base64, captured_at_utc, device_code }` y devuelve `AttendanceProfile` |
| `POST /asistencia/sync` | Backend Spring Boot | Recibe array `[{ event_type: "identify", encrypted_template_base64, encrypted_aes_key_base64, iv_base64, tag_base64, captured_at_utc, device_code, offline_event_id?, retry_count? }]` y devuelve aceptación del lote (`202`) |
| `GET /corehr/biometria` | Backend Spring Boot / Frontend admin | Lista `UserBiometriaListResponse`; no expone templates cifrados |
| `POST /corehr/biometria/enroll/iniciar` | Backend Spring Boot / Frontend admin | Recibe `{ tenant_user_id, device_id, finger_index }` y devuelve `{ enroll_token, device_id, tenant_user_id, finger_index, expires_at }` |
| `GET /corehr/biometria/enroll/pendiente` | Backend Spring Boot | Recibe `device_code` por query y `X-Tenant-ID`; devuelve `{ enroll_token, device_id, device_code, tenant_user_id, finger_index, expires_at }` o `204` |
| `POST /corehr/biometria/enroll/completar` | Backend Spring Boot | Recibe `{ enroll_token, device_code, finger_index, encrypted_template_base64, encrypted_aes_key_base64, iv_base64, tag_base64, captured_at_utc }` y devuelve `UserBiometriaProfile` (`201`) |
| Conexión WebSocket Angular | Frontend | Conectar a `ws://localhost:9001/` y manejar los mensajes JSON del protocolo |
| Configurar `appsettings.json` por tenant | DevOps / Trazzo | Rellenar `Backend:BaseUrl`, `Agent:TenantId`, `Agent:DeviceCode` y `Queue:AgentTokenProtected` con `Configure-Agent.ps1` |
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
├── Trazzo.Biometric.Agent.Tests/        # 345 pruebas xUnit
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
    "KeepAliveIntervalSeconds": 30,
    "DeviceMonitorIntervalSeconds": 5,
    "TenantId": "",
    "DeviceCode": "",
    "MaxIncomingMessageBytes": 262144,
    "MaxPendingOperationsPerClient": 8
  },
  "Backend": {
    "BaseUrl": "",
    "Endpoints": {
      "SecurityPublicKey": "/security/public-key",
      "AttendanceMark": "/asistencia/marcar",
      "AttendanceSync": "/asistencia/sync",
      "BiometricList": "/corehr/biometria",
      "StartEnrollment": "/corehr/biometria/enroll/iniciar",
      "PendingEnrollment": "/corehr/biometria/enroll/pendiente",
      "CompleteEnrollment": "/corehr/biometria/enroll/completar"
    }
  },
  "Biometric": {
    "CaptureTimeoutSeconds": 15,
    "CapturePollingIntervalMilliseconds": 200,
    "PostCaptureCooldownMilliseconds": 200,
    "TemplateBufferSize": 2048,
    "IncludeFingerprintImageInResponses": false,
    "Quality": {
      "MinimumTemplateSize": 400,
      "MinimumForegroundCoveragePercent": 8,
      "MaximumForegroundCoveragePercent": 75,
      "MinimumContrastScore": 18,
      "RequireCenteredFingerprint": true,
      "CenterTolerancePercent": 28,
      "ContrastThresholdOffset": 15
    }
  },
  "Enrollment": {
    "RequiredSamples": 3,
    "SampleTimeoutSeconds": 15,
    "RequireFingerLiftBetweenSamples": false,
    "RemotePollingEnabled": true,
    "RemotePollingIntervalSeconds": 5
  },
  "Security": {
    "BackendPublicKeyUrl": ""
  },
  "Queue": {
    "DatabasePath": "",
    "BackendUrl": "",
    "AgentToken": "",
    "AgentTokenProtected": "",
    "RetryIntervalSeconds": 30
  },
  "AutoUpdate": {
    "Enabled": false,
    "CheckIntervalMinutes": 60,
    "ManifestUrl": "",
    "PublisherCommonName": "Trazzo",
    "MaxMsiBytes": 209715200
  }
}
```

### Calidad de captura

El campo `quality.scorePercent` es el valor recomendado para mostrar en UI como calidad estimada de 0 a 100. `quality.foregroundCoveragePercent` queda como diagnóstico técnico: mide solo el porcentaje de píxeles oscuros dentro del frame crudo del ZK9500, por eso una huella válida puede verse como 20-30% de cobertura del sensor. Cuando `quality.isAcceptable` es `false`, `quality.scorePercent` se limita a 50 como máximo para evitar mostrar alta calidad en una captura rechazada.

Para dedos con poca huella, los umbrales por defecto son más tolerantes (`MinimumForegroundCoveragePercent = 8`, `MinimumContrastScore = 18`) y se sigue exigiendo template válido, contraste mínimo y centrado.

### Tiempo de respuesta ZK9500

Los tiempos están alineados con el demo C# oficial del SDK de ZKTeco (ZKFinger Standard SDK 5.3.0.33, `Demo2/Form1.cs`): el loop `DoCapture` usa `Thread.Sleep(200)` sin cooldown ni espera de lift entre muestras. Por eso la captura e identificación esperan hasta 15 segundos para que el usuario coloque el dedo, con lectura cada 200 ms. No hay bloqueo artificial entre botones: cuando una operación termina por éxito, error o timeout, el usuario puede volver a capturar, identificar o enrolar inmediatamente. `PostCaptureCooldownMilliseconds` (200 ms) es solo drenado técnico interno del lector y no debe devolver error al WebSocket. Por defecto `RequireFingerLiftBetweenSamples = false` (igual que la demo): entre muestras del enrollment el sistema no espera activamente que se levante el dedo, ya que el SDK naturalmente solo devuelve éxito con una colocación nueva. El enrolamiento usa hasta 15 segundos por muestra.

### Valores obligatorios en producción

| Clave | Descripción |
|---|---|
| `Agent:TenantId` | UUID del tenant (institución educativa). Se envía como `X-Tenant-ID` en cada request al backend |
| `Agent:DeviceCode` | Código del lector registrado en CoreHR (`device.code`). Debe coincidir con el valor usado por `/corehr/biometria/enroll/pendiente` |
| `Agent:AllowedOrigins` | Dominios permitidos para el WebSocket, ej. `["https://app.trazzo.pe"]` |
| `Backend:BaseUrl` | Base del backend, ej. `https://api.trazzo.pe/api/v1` |
| `Backend:Endpoints:SecurityPublicKey` | Ruta `GET /security/public-key` para llave RSA pública |
| `Backend:Endpoints:AttendanceMark` | Ruta `POST /asistencia/marcar` para marcación biométrica síncrona |
| `Backend:Endpoints:AttendanceSync` | Ruta `POST /asistencia/sync` para asistencia biométrica offline |
| `Backend:Endpoints:BiometricList` | Ruta `GET /corehr/biometria` para listado admin de registros biométricos; no la consume el agente |
| `Backend:Endpoints:StartEnrollment` | Ruta `POST /corehr/biometria/enroll/iniciar` para inicio admin/frontend del enrolamiento; no la invoca el agente |
| `Backend:Endpoints:PendingEnrollment` | Ruta `GET /corehr/biometria/enroll/pendiente` para polling CoreHR |
| `Backend:Endpoints:CompleteEnrollment` | Ruta `POST /corehr/biometria/enroll/completar` para completar enrolamiento |
| `Queue:AgentTokenProtected` | Token JWT del agente cifrado con DPAPI LocalMachine por `Configure-Agent.ps1` |
| `Queue:AgentToken` | Token JWT legacy en claro. Evitar en producción; se mantiene por compatibilidad |
| `AutoUpdate:Enabled` | `true` para habilitar actualizaciones automáticas |
| `AutoUpdate:ManifestUrl` | URL HTTPS del manifiesto JSON de versiones |
| `AutoUpdate:PublisherCommonName` | CN esperado en el certificado Authenticode del MSI. Default: `Trazzo`. Debe coincidir con el sujeto del certificado usado para firmar |
| `AutoUpdate:MaxMsiBytes` | Tamaño máximo permitido del MSI descargado en bytes. Default: `209715200` (200 MB) |
| `Agent:MaxIncomingMessageBytes` | Tamaño máximo de un mensaje WebSocket entrante. Default: `262144` (256 KB) — cliente que exceda cierra la conexión |
| `Agent:MaxPendingOperationsPerClient` | Máximo de operaciones biométricas en vuelo por conexión WebSocket. Default: `8` — cliente que exceda recibe error, no OOM |

`Security:BackendPublicKeyUrl` y `Queue:BackendUrl` siguen aceptándose como URLs absolutas legacy, pero para nuevas instalaciones se recomienda centralizar rutas en `Backend:BaseUrl` + `Backend:Endpoints`. **Todos los endpoints deben usar HTTPS**; los que no lo hagan se deshabilitan al arrancar (excepto loopback, permitido para desarrollo local).

Para configurar una instalación sin editar `appsettings.json` manualmente, use prompt seguro para no dejar el JWT en historial de consola:

```powershell
$token = Read-Host "Queue:AgentToken" -AsSecureString
& "C:\Program Files\Trazzo\BiometricAgent\Configure-Agent.ps1" `
  -TenantId "00000000-0000-0000-0000-000000000000" `
  -DeviceCode "ZK-C2PRO-00123" `
  -BackendBaseUrl "https://api.trazzo.pe/api/v1" `
  -AgentTokenSecure $token `
  -AllowedOrigins "https://app.trazzo.pe"
```

El script guarda el JWT en `Queue:AgentTokenProtected` usando DPAPI de Windows con alcance `LocalMachine`, limpia `Queue:AgentToken`, crea backup del JSON y endurece ACL del archivo para `SYSTEM` y Administradores.

### Manifiesto de auto-update

El auto-updater consume un JSON en `AutoUpdate:ManifestUrl` con este formato:

```json
{
  "version": "1.1.0",
  "downloadUrl": "https://releases.trazzo.pe/middleware/TrazzoAgent-1.1.0.msi",
  "sha256": "a3f1c2d4e5..."
}
```

**Cadena de verificación antes de instalar** (todo debe pasar; caso contrario el MSI se descarta):

1. La URL del manifiesto y del MSI deben ser HTTPS.
2. La versión del manifiesto debe ser **mayor** a la instalada (previene downgrade attacks).
3. `Content-Length` del MSI debe estar presente y ≤ `AutoUpdate:MaxMsiBytes` (default 200 MB).
4. SHA-256 del binario descargado debe coincidir con el declarado en el manifiesto.
5. **Firma Authenticode**: el MSI debe estar firmado y el CN del certificado debe coincidir con `AutoUpdate:PublisherCommonName` (default `Trazzo`).
6. La cadena de certificados debe validar contra el trust store de Windows (revocación online, root confiable).

Sin firma válida no se ejecuta el `msiexec`. Adicionalmente, el directorio de updates conserva solo los últimos 3 MSIs; los anteriores se borran para no acumular espacio en disco.

---

## Compilar y Ejecutar Tests

```powershell
dotnet build .\Trazzo.Middleware.slnx -c Release
dotnet test
```

Los 345 tests no necesitan hardware. Usan implementaciones falsas (fakes) del SDK biométrico. En entornos restringidos, los tests de `LocalWebSocketServerServiceTests` pueden fallar al abrir `HttpListener`; ejecutar `dotnet test --filter "FullyQualifiedName!~LocalWebSocketServerServiceTests"` valida los que no dependen del listener local.

---

## Generar el MSI

```powershell
dotnet build .\Trazzo.Biometric.Agent.Installer\Trazzo.Biometric.Agent.Installer.wixproj -c Release
```

El MSI queda en:

```text
Trazzo.Biometric.Agent.Installer\bin\Release\es-ES\Trazzo.Biometric.Agent.msi
Trazzo.Biometric.Agent.Installer\bin\Release\en-US\Trazzo.Biometric.Agent.msi
```

El MSI es **self-contained**: incluye el runtime de .NET 10. La PC del colegio no necesita tener .NET instalado.

---

## CI/CD con GitHub Actions

El pipeline `.github/workflows/middleware-ci.yml` se activa automáticamente en push (ramas `master` y `feature/**`) y en pull requests hacia `master`.

### Jobs

| Job | Trigger | Runner | Descripción |
|---|---|---|---|
| `build-test-analyze` | push y PR | Windows | Compila, ejecuta los 345 tests con cobertura (Coverlet) y envía análisis a SonarCloud. En push también publica el agente self-contained y sube los binarios como artefacto. |
| `package-msi` | solo push | Windows | Descarga los binarios del job anterior y genera el MSI con WiX v4 (`-p:SkipAgentPublish=true`). No re-publica el agente. |

El job `package-msi` depende de `build-test-analyze` y sólo corre en push, por lo que en pull requests sólo se ejecuta el primer job.

### Distribución a los colegios

Actualmente el MSI generado por CI queda como artefacto interno de GitHub Actions — solo accesible para miembros del repositorio. Para que el técnico del colegio pueda descargarlo con un simple link, falta agregar un paso al workflow que publique en **GitHub Releases**:

```
Push a master
  → CI compila y ejecuta 345 tests
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
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\es-ES\Trazzo.Biometric.Agent.msi
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
| Identificar huella | Captura para asistencia: intenta `POST /asistencia/marcar`; si falla, encola el evento para `POST /asistencia/sync` |
| Enrolar huella | Captura 3 muestras consecutivas y genera template definitivo con DBMerge |
| Cancelar enrolamiento | Interrumpe un enrolamiento en curso |
| Limpiar | Limpia el log y resetea la UI |

**Tutorial integrado:** la primera vez que se abre la página aparece automáticamente un tutorial de 6 pasos. Se puede cerrar y volver a abrir desde el botón `? Tutorial` en la esquina superior derecha. La preferencia se guarda en `localStorage`.

**Sobre `templateBase64: null`:** es el comportamiento correcto en producción. Hay dos escenarios que producen `null`:

1. **Con clave RSA configurada (producción)**: el agente cifró el template. Los datos reales están en `encryptedTemplate`.
2. **Sin clave RSA configurada**: el guard de seguridad bloquea la salida plaintext. Los datos NO se transmiten.

Para desarrollo local sin backend, se puede reactivar el fallback plaintext con la env var y borrar el caché:

```powershell
$env:TRAZZO_ALLOW_PLAINTEXT_TEMPLATES = "true"
Remove-Item "$env:ProgramData\TrazzoAgent" -Recurse -Force
dotnet run --project .\Trazzo.Biometric.Agent\Trazzo.Biometric.Agent.csproj
```

⚠️ **Nunca setees `TRAZZO_ALLOW_PLAINTEXT_TEMPLATES=true` en producción.** Es solo para debugging local sin backend.

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

Captura simple. Devuelve el template cifrado para diagnóstico o flujos locales; no registra asistencia ni encola eventos offline.

### fingerprint.identify

```json
{ "type": "fingerprint.identify" }
```

Captura para asistencia. Intenta `POST /asistencia/marcar` inmediatamente; si el backend no está disponible o rechaza la llamada, encola el evento cifrado para reenvío por `POST /asistencia/sync`.

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
- La clave AES se cifra con RSA-2048-OAEP-SHA256 usando la clave pública del backend.
- **Por defecto**, si no hay clave RSA configurada, el agente NO transmite el template: los mensajes salen con `templateBase64: null`. Nunca se filtra plaintext biométrico por descuido operativo.
- Para modo desarrollo local sin backend RSA, se puede reactivar el fallback plaintext con la variable de entorno `TRAZZO_ALLOW_PLAINTEXT_TEMPLATES=true`.

**Flujo de cifrado por captura:**

```
Huella física
  → SDK extrae template (binario, en RAM)
  → AES-256-GCM cifra el template (clave efímera por captura)
  → RSA-2048-OAEP-SHA256 cifra la clave AES (con clave pública del backend)
  → Se transmite: { encrypted_template_base64, encrypted_aes_key_base64, iv_base64, tag_base64 }
  → Imagen cruda descartada
```

---

## Endurecimiento de Seguridad

Además del cifrado híbrido, el agente aplica los siguientes controles defensivos:

### Transporte

- **HTTPS obligatorio** en todos los endpoints de backend. Cualquier URL HTTP no-loopback se rechaza al arrancar (`BackendEndpointResolver.EnsureSecureUrl`).
- **Loopback aceptado** solo para desarrollo local (`http://localhost`, `http://127.0.0.1`).
- **DPAPI LocalMachine** protege el token JWT en `Queue:AgentTokenProtected` (via `Configure-Agent.ps1`).

### WebSocket local

- **Cap de tamaño de mensaje** (256 KB por defecto) evita OOM ante clientes maliciosos que envíen fragmentos infinitos.
- **Cap de operaciones concurrentes por cliente** (8 por defecto) evita flood attacks locales.
- **Restricción por Origin** (`Agent:AllowedOrigins`) — sin lista configurada, solo se aceptan orígenes loopback.

### Templates biométricos

- **Guard plaintext** cerrado por defecto: `templateBase64: null` si el cifrado no está configurado (opt-in explícito con env var para dev).
- **Validación de KeySize RSA ≥ 2048 bits**: claves más chicas se rechazan al importarse.
- **Cache de clave pública** en `%PROGRAMDATA%\TrazzoAgent\public_key.cache` protegido con DPAPI-LocalMachine y marcador de integridad `TRAZZO_KEY_CACHE_V1`. Cache manipulado o de versión previa se descarta.
- **Response size limits** en todos los HttpClients al backend (32 KB para public key, 8-16 KB para respuestas de API).

### Auto-update

- Verificación en cadena: HTTPS + versión mayor + `Content-Length` ≤ cap + SHA-256 + **firma Authenticode** con CN esperado + cadena de certificados válida.
- Cap de tamaño del MSI (default 200 MB) previene llenar disco con descarga malformada.
- Cleanup automático mantiene solo los últimos 3 MSIs descargados.

### SDK ZKTeco

- Carga de `libzkfpcsharp.dll` **solo desde** `AppContext.BaseDirectory\Native\x64\`. Ya no hay fallback a `Environment.CurrentDirectory` (mitiga DLL hijacking si un atacante controla el cwd del servicio SYSTEM).

### Cola offline

- SQLite con **WAL journal mode** y `synchronous=NORMAL` (mejor concurrencia + durabilidad).
- **Connection pooling** habilitado.
- **Batch UPDATE** (`WHERE id IN (...)`) para marcar eventos como enviados en una sola sentencia.
- **Prune throttling**: la limpieza de eventos viejos corre máximo una vez cada 6 h (antes corría en cada ciclo del forwarder).
- Eventos no soportados o sin `device_code` se marcan como **fallidos** (auditables), no descartados silenciosamente.

### Enrolamiento remoto

- Se valida `pending.ExpiresAt` antes de procesar la sesión — sesiones expiradas se ignoran.

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
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\es-ES\Trazzo.Biometric.Agent.msi

# Instalar MSI silencioso
msiexec /i Trazzo.Biometric.Agent.msi /quiet /norestart

# Detener / iniciar servicio
Stop-Service -Name TrazzoAgent
Start-Service -Name TrazzoAgent

# Desinstalar
msiexec /x .\Trazzo.Biometric.Agent.Installer\bin\Release\es-ES\Trazzo.Biometric.Agent.msi
```
