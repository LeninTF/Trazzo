# Middleware Dev Handoff

Este documento resume el estado actual del middleware y lo que falta para cerrar la integracion end-to-end con backend, frontend y despliegue.

## Estado Actual

El middleware ya compila y genera MSI Release. El agente tiene configuradas las 6 rutas del modulo biometrico/asistencia en `appsettings.json`:

| Contrato OpenAPI | Estado middleware |
|---|---|
| `POST /asistencia/marcar` | Implementado. `fingerprint.identify` intenta marcacion sincrona primero. |
| `POST /asistencia/sync` | Implementado. Fallback offline desde cola SQLite. |
| `GET /corehr/biometria` | Configurado/documentado. Es admin/frontend; el agente no lo consume. |
| `POST /corehr/biometria/enroll/iniciar` | Configurado/documentado. Es admin/frontend; el agente no lo invoca. |
| `GET /corehr/biometria/enroll/pendiente` | Implementado. Polling por `device_code`. |
| `POST /corehr/biometria/enroll/completar` | Implementado. Envia template enrolado cifrado. |

Campos enviados por asistencia:

```json
{
  "event_type": "identify",
  "encrypted_template_base64": "...",
  "encrypted_aes_key_base64": "...",
  "iv_base64": "...",
  "tag_base64": "...",
  "captured_at_utc": "2026-07-01T00:00:00.0000000Z",
  "device_code": "ZK-C2PRO-00123"
}
```

Campos enviados por `enroll/completar`:

```json
{
  "enroll_token": "...",
  "device_code": "ZK-C2PRO-00123",
  "finger_index": 1,
  "encrypted_template_base64": "...",
  "encrypted_aes_key_base64": "...",
  "iv_base64": "...",
  "tag_base64": "...",
  "captured_at_utc": "2026-07-01T00:00:00.0000000Z"
}
```

## Pendiente Para Dev

Estos puntos no bloquean el MSI actual. Son tareas de integración/decisión para cerrar el flujo real con backend, frontend y hardware.

### 1. WebSocket local debe decidir si expone respuesta del backend

Hoy el WebSocket local devuelve el resultado de captura del agente (`fingerprint.identify.result`) y el middleware usa el backend solo como exito/fallo HTTP.

Pendiente si frontend necesita mostrar datos del backend:

- Parsear la respuesta de `POST /asistencia/marcar` (`AttendanceProfile`).
- Incluir esa respuesta, o un resumen, en el mensaje WebSocket de `fingerprint.identify`.
- Definir comportamiento cuando `/asistencia/marcar` falla y el evento queda en cola offline: el frontend debe ver "pendiente de sincronizacion", no asistencia confirmada.
- Agregar tests para respuesta WebSocket con asistencia confirmada y con fallback offline.

No cambiar esto si el frontend solo necesita saber que la captura fue tomada y el backend maneja la notificacion por sus propios canales.

### 2. Validacion con backend real

Probar contra ambiente real/staging:

- `GET /security/public-key` devuelve `{ publicKey, kid }`.
- `POST /asistencia/marcar` acepta payload `identify` y responde `AttendanceProfile`.
- `POST /asistencia/sync` acepta array y responde `202`.
- `GET /corehr/biometria/enroll/pendiente?device_code=...` devuelve `204` sin sesion y `200` con sesion.
- `POST /corehr/biometria/enroll/completar` responde `201` y crea `UserBiometriaProfile`.
- Backend rechaza correctamente dispositivos inactivos (`device.state = false`).
- Backend valida `X-Tenant-ID`, token bearer del agente y `device_code`.

### 3. Validacion con hardware real

Probar en Windows 10/11 x64 con:

- Driver/software oficial ZKTeco instalado.
- `libzkfpcsharp.dll` x64 presente.
- Lector ZK9500 conectado por USB.
- Captura simple.
- Identificacion para asistencia.
- Enrolamiento remoto iniciado desde frontend/admin.
- Desconexion/reconexion del lector.

El WebSocket ya no aplica rate limit artificial entre `fingerprint.capture`, `fingerprint.identify` y `fingerprint.enroll.start`. Si una captura termina por exito, error o timeout, el siguiente comando puede enviarse inmediatamente. El drenado post-captura del ZK9500 queda como espera tecnica interna del SDK y no debe responder error al cliente.

### 4. Configuracion por tenant antes de entregar MSI

No es obligatorio editar `appsettings.json` a mano. El publish/MSI incluye `Configure-Agent.ps1`, que actualiza el JSON instalado, crea backup y reinicia el servicio. El JWT no queda en claro: se guarda en `Queue:AgentTokenProtected` cifrado con DPAPI `LocalMachine` y el script limpia `Queue:AgentToken`.

Ejemplo:

```powershell
$token = Read-Host "Queue:AgentToken" -AsSecureString
& "C:\Program Files\Trazzo\BiometricAgent\Configure-Agent.ps1" `
  -TenantId "00000000-0000-0000-0000-000000000000" `
  -DeviceCode "ZK-C2PRO-00123" `
  -BackendBaseUrl "https://api.trazzo.pe/api/v1" `
  -AgentTokenSecure $token `
  -AllowedOrigins "https://app.trazzo.pe"
```

El resultado esperado en `appsettings.json` es:

```json
{
  "Agent": {
    "TenantId": "uuid-del-tenant",
    "DeviceCode": "codigo-del-device-en-corehr",
    "AllowedOrigins": ["https://app.trazzo.pe"]
  },
  "Backend": {
    "BaseUrl": "https://api.trazzo.pe/api/v1"
  },
  "Queue": {
    "AgentToken": "",
    "AgentTokenProtected": "dpapi-localmachine:..."
  }
}
```

`Agent:DeviceCode` debe coincidir con `device.code` en CoreHR. Si falta, el agente no debe enviar payloads invalidos al backend.

El script tambien endurece ACL del archivo de configuracion para `SYSTEM` y Administradores. Si se quiere capturar estos valores dentro del instalador visual, falta crear una pantalla custom WiX y una accion custom que escriba el JSON sin exponer el JWT en propiedades/logs MSI.

### 5. Despliegue y actualizaciones

Pendiente de infraestructura/release:

- Publicar MSI en GitHub Releases o storage con URL estable.
- Publicar manifiesto de auto-update HTTPS.
- Firmar digitalmente el MSI para reducir alertas de SmartScreen.
- Definir como se inyecta/configura `appsettings.json` por tenant en instalacion real.

### 6. Tests de entorno

La suite tiene 262 tests. En este entorno se verifico:

- `dotnet build Trazzo.Biometric.Agent.Tests\Trazzo.Biometric.Agent.Tests.csproj --no-restore`: OK.
- `dotnet test ... --filter "FullyQualifiedName~WebSocketResponseTests|FullyQualifiedName~ZKTecoScannerServiceTests"`: 89/89 OK.
- `dotnet test ... --filter "FullyQualifiedName!~LocalWebSocketServerServiceTests"`: 240/240 OK.

Los 22 tests que levantan `HttpListener` pueden fallar en entornos restringidos con:

```text
System.Net.HttpListenerException: Controlador no valido.
```

Eso es restriccion del entorno de Windows/runner, no necesariamente bug funcional del middleware. Validar esos tests en una maquina Windows con permisos normales para abrir listener local.

## MSI Actual

El ultimo build local genero:

```text
Trazzo.Biometric.Agent.Installer\bin\Release\es-ES\Trazzo.Biometric.Agent.msi
Trazzo.Biometric.Agent.Installer\bin\Release\en-US\Trazzo.Biometric.Agent.msi
```

Hashes SHA-256:

```text
es-ES: A4298480CD4358D396EFB40A72F348CE1FE9CE59CE099E57013A6866FC12C454
en-US: 5144C298E8840988093AF7CDE2EB9A71DE8D30F055F1E73AEF065334C7D53D74
```

Comando:

```powershell
dotnet build .\Trazzo.Biometric.Agent.Installer\Trazzo.Biometric.Agent.Installer.wixproj -c Release
```
