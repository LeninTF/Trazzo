# Trazzo Biometric Agent

Middleware local de Windows para conectar el lector biométrico **ZKTeco ZK9500** con Trazzo.

El agente corre como un Windows Service y expone un WebSocket local en:

```text
ws://localhost:9001/
```

El frontend se conecta a ese WebSocket, solicita una operación biométrica y el agente se encarga de hablar con el lector por USB. Cuando captura una huella, genera el template biométrico, lo cifra si la clave pública del backend está configurada y deja el evento en una cola local si el backend no está disponible.

## Estado Actual

El middleware ya tiene:

- Servicio Windows instalable por MSI.
- Integración con ZKTeco ZK9500 usando `libzkfpcsharp.dll`.
- WebSocket local para el frontend.
- Captura simple, identificación y enrolamiento de huella.
- Validación de calidad de huella.
- Cifrado híbrido AES-256-GCM + RSA-OAEP.
- Cola offline con SQLite.
- Reintento automático con backoff.
- Restricción opcional por origen WebSocket.
- Página local de prueba: `scripts/test-websocket.html`.

## Estructura

La solución contiene tres proyectos:

```text
Trazzo.Biometric.Agent
Trazzo.Biometric.Agent.Tests
Trazzo.Biometric.Agent.Installer
```

`Trazzo.Biometric.Agent` es el servicio real.

`Trazzo.Biometric.Agent.Tests` contiene las pruebas xUnit.

`Trazzo.Biometric.Agent.Installer` genera el MSI con WiX Toolset v4.

## Requisitos

- Windows 10 o superior.
- .NET SDK 10.
- Lector ZKTeco ZK9500 conectado por USB.
- SDK oficial ZKFinger para Windows.
- DLL del SDK:

```text
Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll
```

La DLL no debe asumirse como parte pública del repositorio porque pertenece al SDK de ZKTeco. Para generar el MSI completo, debe existir antes del build.

En esta máquina se usó:

```text
C:\Users\PC\Downloads\KZFingerSDK\ZKFingerSDK_Windows_Standard\ZKFinger Standard SDK 5.3.0.33\C#\lib\x64\libzkfpcsharp.dll
```

## Preparar la DLL de ZKTeco

Si la DLL no está en el proyecto, cópiala así:

```powershell
Copy-Item `
  -LiteralPath "C:\Users\PC\Downloads\KZFingerSDK\ZKFingerSDK_Windows_Standard\ZKFinger Standard SDK 5.3.0.33\C#\lib\x64\libzkfpcsharp.dll" `
  -Destination ".\Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll" `
  -Force
```

Verifica que quedó en su lugar:

```powershell
Test-Path .\Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll
```

Debe devolver:

```text
True
```

## Configuración

La configuración principal está en:

```text
Trazzo.Biometric.Agent\appsettings.json
```

Valores importantes:

```json
{
  "Agent": {
    "WebSocketUrl": "http://localhost:9001/",
    "AllowedOrigins": []
  },
  "Security": {
    "BackendPublicKeyUrl": ""
  },
  "Queue": {
    "DatabasePath": "",
    "BackendUrl": "",
    "AgentToken": "",
    "RetryIntervalSeconds": 30
  }
}
```

En desarrollo puedes dejar `BackendPublicKeyUrl` y `BackendUrl` vacíos. El agente funciona, pero los templates pueden viajar en Base64 plano y no se reenvían eventos al backend.

En producción deben configurarse:

- `Security:BackendPublicKeyUrl`: endpoint que entrega la clave pública RSA.
- `Queue:BackendUrl`: endpoint del backend que recibe eventos offline.
- `Queue:AgentToken`: token del agente si el backend lo exige.
- `Agent:AllowedOrigins`: dominios permitidos para conectarse al WebSocket.

## Generar el MSI

Desde la raíz del middleware:

```powershell
.\Trazzo.Biometric.Agent\scripts\Install-Agent.ps1 -BuildMsi
```

Ese comando hace todo el flujo:

1. Ejecuta `dotnet publish -c Release` del agente.
2. Copia la salida publicada al proyecto WiX.
3. Incluye `Native\x64\libzkfpcsharp.dll` si existe.
4. Genera el MSI.

El instalador queda en:

```text
Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```

El MSI instala en:

```text
C:\Program Files\Trazzo\BiometricAgent\
```

Estructura esperada:

```text
C:\Program Files\Trazzo\BiometricAgent\
  Trazzo.Biometric.Agent.exe
  appsettings.json
  Native\x64\libzkfpcsharp.dll
  *.dll
  *.json
```

## CI/CD con GitHub Actions

El repositorio incluye un pipeline en `.github/workflows/middleware-ci.yml` que se activa automáticamente en cada push o pull request que toque la carpeta `middleware/`.

El pipeline tiene dos etapas:

1. **Tests** — compila el proyecto y ejecuta los 26 tests xUnit. No necesita la DLL porque usa implementaciones falsas.
2. **Build MSI** — solo corre si los tests pasan. Descifra la DLL del SDK desde un secret de GitHub, genera el MSI con WiX Toolset v4 y lo sube como artefacto descargable.

El MSI generado queda disponible en la pestaña **Actions** del repositorio bajo el nombre:

```text
Trazzo.Biometric.Agent-buildN.msi
```

### Configurar el secret ZKFP_DLL_BASE64

La DLL `libzkfpcsharp.dll` es propietaria de ZKTeco y no puede subirse al repositorio. El pipeline la recibe como un secret en Base64.

**Paso 1 — Generar el valor Base64**

Abre PowerShell en tu máquina local y ejecuta:

```powershell
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes(
        "\KZFingerSDK\ZKFingerSDK_Windows_Standard\ZKFinger Standard SDK 5.3.0.33\C#\lib\x64\libzkfpcsharp.dll"
    )
) | Set-Clipboard
```

El comando no muestra nada. El valor ya quedó en el clipboard.

**Paso 2 — Registrar el secret en GitHub**

1. Ve a **Settings** del repositorio en GitHub.
2. En el menú izquierdo: **Secrets and variables → Actions**.
3. Clic en **New repository secret**.
4. Completa los campos:
   - **Name:** `ZKFP_DLL_BASE64`
   - **Secret:** Ctrl+V (pega el contenido del clipboard)
5. Clic en **Add secret**.

A partir de ese momento, cualquier push al middleware genera el MSI automáticamente en GitHub Actions.

## Instalar Como Servicio

El MSI registra el servicio automáticamente. Debe instalarse como administrador porque es una instalación per-machine y registra un Windows Service.

Ejecuta PowerShell como administrador:

```powershell
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```

También puedes instalarlo en modo silencioso:

```powershell
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi /qn
```

Servicio instalado:

```text
Nombre interno : TrazzoAgent
Display name   : Trazzo Biometric Agent
Inicio         : Automatic
Cuenta         : LocalSystem
```

El servicio arranca automáticamente al terminar la instalación.

## Verificar Que Está Corriendo

Revisa el servicio:

```powershell
Get-Service -Name TrazzoAgent
```

Debe estar en:

```text
Running
```

Revisa el puerto WebSocket:

```powershell
Get-NetTCPConnection -LocalPort 9001
```

Debe existir un listener en `9001`.

También puedes probar el endpoint HTTP de salud:

```powershell
Invoke-RestMethod http://localhost:9001/health
```

## Probar Con El HTML Local

Abre:

```text
Trazzo.Biometric.Agent\scripts\test-websocket.html
```

Prueba en este orden:

1. `health.check`
2. `device.status`
3. `fingerprint.capture`
4. `fingerprint.identify`
5. `fingerprint.enroll.start`

Si el HTML muestra “WebSocket desconectado”, significa que no hay servicio escuchando en `ws://localhost:9001/`.

En ese caso revisa:

```powershell
Get-Service -Name TrazzoAgent
Get-NetTCPConnection -LocalPort 9001
```

## Probar Sin Instalar

Para desarrollo rápido puedes correr el agente desde consola:

```powershell
dotnet run --project .\Trazzo.Biometric.Agent\Trazzo.Biometric.Agent.csproj --configuration Release
```

Déjalo abierto mientras pruebas `scripts/test-websocket.html`.

Esta forma no registra el servicio. Solo sirve para pruebas locales.

## Mensajes WebSocket

### health.check

```json
{ "type": "health.check" }
```

Respuesta esperada:

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

Respuesta esperada con lector conectado:

```json
{
  "type": "device.status.result",
  "success": true,
  "isSdkAvailable": true,
  "isInitialized": true,
  "isDeviceOpen": true,
  "isConnected": true,
  "deviceCount": 1,
  "message": "Lector biométrico conectado."
}
```

### fingerprint.capture

```json
{ "type": "fingerprint.capture" }
```

Hace una captura simple. Si el cifrado está activo, devuelve `encryptedTemplate`. Si no hay clave RSA configurada, devuelve `templateBase64`.

### fingerprint.identify

```json
{ "type": "fingerprint.identify" }
```

Captura una huella para asistencia. No hace `DBMerge`. Si hay cifrado activo, el evento también queda en cola offline.

### fingerprint.enroll.start

```json
{ "type": "fingerprint.enroll.start" }
```

Hace enrolamiento con tres muestras válidas y luego fusiona las muestras con `DBMerge`.

### fingerprint.enroll.cancel

```json
{ "type": "fingerprint.enroll.cancel" }
```

Cancela un enrolamiento en progreso.

### queue.status

```json
{ "type": "queue.status" }
```

Devuelve cuántos eventos siguen pendientes de reenvío al backend.

## Seguridad

El agente usa cifrado híbrido:

1. Descarga una clave pública RSA desde `Security:BackendPublicKeyUrl`.
2. Genera una clave AES-256 por captura.
3. Cifra el template con AES-256-GCM.
4. Cifra la clave AES con RSA-OAEP.
5. Envía ciphertext, clave AES cifrada, IV y tag.

Si no puede descargar la clave pública, intenta usar cache local:

```text
%PROGRAMDATA%\TrazzoAgent\public_key.cache
```

Si no hay endpoint ni cache, el agente arranca en modo desarrollo sin cifrado. Esto no debe usarse en producción.

## Cola Offline

Cuando hay cifrado activo, los eventos biométricos exitosos se guardan en:

```text
%PROGRAMDATA%\TrazzoAgent\events.db
```

El servicio `EventForwarderService` reintenta enviarlos al backend. Si el backend falla, usa backoff exponencial con jitter. Después de varios intentos fallidos, el evento queda marcado como `failed`.

## Calidad De Huella

El SDK puede generar un template aunque la huella esté mal colocada. Por eso el agente valida:

- Tamaño mínimo del template.
- Cobertura de área oscura.
- Contraste.
- Posición centrada del dedo.

Configuración recomendada:

```json
{
  "Biometric": {
    "Quality": {
      "MinimumTemplateSize": 400,
      "MinimumForegroundCoveragePercent": 18,
      "MaximumForegroundCoveragePercent": 75,
      "MinimumContrastScore": 25,
      "RequireCenteredFingerprint": true,
      "CenterTolerancePercent": 28
    }
  }
}
```

Estos valores deben ajustarse con pruebas reales del lector y usuarios.

## Control De Sesión

El agente solo captura cuando una operación fue iniciada por WebSocket. Si alguien deja el dedo sobre el lector sin operación activa, la lectura se ignora.

Estados principales:

| Estado | Significado |
| --- | --- |
| `Idle` | Listo para recibir una operación |
| `Capturing` | Captura simple en progreso |
| `Identifying` | Identificación en progreso |
| `Enrolling` | Enrolamiento en progreso |
| `Cooldown` | Pausa corta después de una operación |

Solo se permite una operación biométrica a la vez.

## Compatibilidad Validada

El desarrollo actual está pensado para:

- ZKTeco ZK9500.
- ZKFingerSDK para Windows.
- Template ZKTeco Finger V10.0.
- Imagen del sensor consultada desde el SDK.
- Fallback documentado para ZK9500: 300 x 400 px.

Durante prueba real en esta máquina, el lector respondió correctamente:

```text
DeviceId : ZK9500-1967251700027
Estado   : conectado
SDK      : inicializado
WebSocket: ws://localhost:9001/
```

## Errores Comunes

### WebSocket desconectado

No hay agente escuchando en `localhost:9001`.

Revisa:

```powershell
Get-Service -Name TrazzoAgent
Get-NetTCPConnection -LocalPort 9001
```

### No se encontró la DLL del SDK

Falta:

```text
Native\x64\libzkfpcsharp.dll
```

Copia la DLL oficial del SDK y vuelve a generar el MSI.

### No se encontró ningún lector biométrico

Posibles causas:

- El ZK9500 no está conectado.
- Windows no reconoce el dispositivo.
- Otro proceso de ZKTeco tiene ocupado el lector.

Procesos que conviene cerrar si interfieren:

```powershell
taskkill /F /IM ISSOnline_App.exe
taskkill /F /IM ISSOnline.exe
taskkill /F /IM iZHost.exe
taskkill /F /IM ZKOnlineProtect.exe
```

### Error MSI 1925

El MSI necesita privilegios de administrador porque instala para todos los usuarios y registra un servicio.

Ejecuta PowerShell como administrador y vuelve a instalar.

### Error MSI 1603

Revisa el log del MSI. En este proyecto, la causa más común fue instalar sin privilegios elevados.

Ejemplo con log:

```powershell
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi /l*v install.log
```

## Comandos Útiles

Compilar todo:

```powershell
dotnet build .\Trazzo.Middleware.slnx -c Release
```

Ejecutar pruebas:

```powershell
dotnet test --no-restore --no-build
```

Generar MSI:

```powershell
.\Trazzo.Biometric.Agent\scripts\Install-Agent.ps1 -BuildMsi
```

Instalar MSI:

```powershell
msiexec /i .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```

Detener servicio:

```powershell
Stop-Service -Name TrazzoAgent
```

Iniciar servicio:

```powershell
Start-Service -Name TrazzoAgent
```

Desinstalar MSI:

```powershell
msiexec /x .\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
```

## Pendiente

- Firmar digitalmente el MSI para entornos corporativos.
- Leer la versión del MSI desde la versión del ensamblado en lugar de dejar `1.0.0` fijo.
- Implementar un auto-updater silencioso si se requiere despliegue masivo.
