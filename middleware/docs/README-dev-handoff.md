# Middleware Dev Handoff

Este documento resume el estado actual del middleware y lo que falta para cerrar la integracion end-to-end con backend, frontend y despliegue.

## Estado Actual

El middleware ya compila y genera MSI Release. El agente tiene configuradas en `appsettings.json` (`Backend:Endpoints`) las 7 rutas del modulo biometrico/asistencia. Las que llama el agente van con `Authorization: Bearer trzd_...`, el token del dispositivo que emite el panel (pantalla "Dispositivos"); el backend solo acepta ese token en las rutas del agente.

| Contrato OpenAPI | Estado middleware |
|---|---|
| `GET /security/public-key` | Implementado. Devuelve `{ publicKey, kid }`; con `Security:PinnedPublicKeySha256` el agente rechaza una clave con otra huella. |
| `POST /asistencia/marcar` | Implementado. `fingerprint.identify` intenta marcacion sincrona primero. |
| `POST /asistencia/sync` | Implementado. Fallback offline desde cola SQLite. |
| `GET /corehr/biometria` | Configurado/documentado. Es del panel (JWT); el agente no lo consume. |
| `POST /corehr/biometria/enroll/iniciar` | Configurado/documentado. Es del panel (JWT); el agente no lo invoca. |
| `GET /corehr/biometria/enroll/pendiente` | Implementado. Polling por `device_code`. |
| `POST /corehr/biometria/enroll/completar` | Implementado. Envia template enrolado cifrado. |

El cifrado (RSA-OAEP + AES-GCM, `iv` y `tag` separados) es el de la seccion "Criptografia biometrica (normativa)" de `openapi.yaml`. `captured_at_utc` va en UTC y sin offset, porque el backend lo lee como `LocalDateTime`.

Campos enviados por asistencia:

```json
{
  "event_type": "identify",
  "encrypted_template_base64": "...",
  "encrypted_aes_key_base64": "...",
  "iv_base64": "...",
  "tag_base64": "...",
  "captured_at_utc": "2026-07-01T00:00:00.000",
  "device_code": "ZK-C2PRO-00123"
}
```

En `/asistencia/sync` el cuerpo es un array de esos objetos, y cada uno lleva ademas `offline_event_id` y `retry_count` (trazabilidad de la cola offline).

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
  "captured_at_utc": "2026-07-01T00:00:00.000"
}
```

## Pendiente Para Dev

Estos puntos no bloquean el MSI actual. Son tareas de integración/decisión para cerrar el flujo real con backend, frontend y hardware.

### 1. WebSocket local: respuesta del backend

`fingerprint.identify.result` ya dice que paso con la marcacion: `deliveryStatus` es `marked` (el backend la registro), `queued` (quedo en la cola offline) o `not_transmitted` (no se envio, p. ej. sin clave RSA), y `matched`, `matchedUserReference` y `matchScore` dicen si la huella coincidio con una enrolada. El frontend puede distinguir "asistencia registrada" de "solo se capturo la huella".

Pendiente solo si el frontend necesita mostrar datos del backend:

- Parsear la respuesta de `POST /asistencia/marcar` (`AttendanceProfile`).
- Incluir esa respuesta, o un resumen, en el mensaje WebSocket de `fingerprint.identify`.
- Agregar tests para respuesta WebSocket con asistencia confirmada y con fallback offline.

No cambiar esto si al frontend le basta `deliveryStatus` y el backend maneja la notificacion por sus propios canales.

### 2. Validacion con backend real

Hecho contra el backend real levantado en Docker (stack local), con un agente simulado que cifra como el middleware:

- Emision del token del dispositivo (el backend guarda solo su SHA-256), 401 sin token o con token revocado.
- `enroll/iniciar` + `enroll/pendiente` + `enroll/completar` con un template SourceAFIS generado en .NET.
- La clave RSA se conserva al reiniciar el backend.
- `POST /asistencia/marcar` responde 200 (`PUNTUAL`, `check_in` en hora de Lima) y el reenvio es idempotente.
- `POST /asistencia/sync` responde 202 con el resultado por item, y 422 si ningun evento del lote se pudo registrar (el agente lo reintenta).
- El token fija tenant y lector: un `device_code` distinto del del token responde 403.

Falta repetirlo en staging o produccion con el MSI instalado y el lector real.

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

No es obligatorio editar `appsettings.json` a mano. El publish/MSI incluye `Configure-Agent.ps1`, que actualiza el JSON instalado, crea backup y reinicia el servicio. El token del dispositivo no queda en claro: se guarda en `Queue:AgentTokenProtected` cifrado con DPAPI `LocalMachine` y el script limpia `Queue:AgentToken`.

El panel genera el comando al emitir el token (Dispositivos → Emitir token), con la huella de la clave publica del backend. Se ejecuta en PowerShell como administrador, en `C:\Program Files\Trazzo\BiometricAgent\`:

```powershell
.\Configure-Agent.ps1 -TenantId '00000000-0000-0000-0000-000000000000' -DeviceCode 'ZK-C2PRO-00123' -BackendBaseUrl 'https://trazzosaas.noahtechperu.com/api/v1' -PinnedPublicKeySha256 '<huella-sha256-del-panel>' -AgentTokenSecure (Read-Host "AgentToken" -AsSecureString)
```

`-AllowedOrigins` vale por defecto `https://trazzosaas.noahtechperu.com`; para pruebas locales se anade `http://localhost:4200` (ng serve) o `http://localhost:4000` (docker-compose.local.yml).

El resultado esperado en `appsettings.json` es:

```json
{
  "Agent": {
    "TenantId": "uuid-del-tenant",
    "DeviceCode": "codigo-del-device-en-corehr",
    "AllowedOrigins": ["https://trazzosaas.noahtechperu.com"]
  },
  "Backend": {
    "BaseUrl": "https://trazzosaas.noahtechperu.com/api/v1"
  },
  "Security": {
    "PinnedPublicKeySha256": ["<huella-sha256-del-panel>"]
  },
  "Queue": {
    "AgentToken": "",
    "AgentTokenProtected": "dpapi-localmachine:..."
  }
}
```

`Agent:DeviceCode` debe coincidir con `device.code` en CoreHR. Si falta, el agente no debe enviar payloads invalidos al backend.

El script tambien endurece ACL del archivo de configuracion para `SYSTEM` y Administradores. Si se quiere capturar estos valores dentro del instalador visual, falta crear una pantalla custom WiX y una accion custom que escriba el JSON sin exponer el token en propiedades/logs MSI.

### 5. Despliegue y actualizaciones

Pendiente de infraestructura/release:

- Publicar MSI en GitHub Releases o storage con URL estable.
- Publicar manifiesto de auto-update HTTPS.
- Firmar digitalmente el MSI: reduce las alertas de SmartScreen y el auto-update (apagado por defecto, `AutoUpdate:Enabled`) solo instala un MSI con firma Authenticode del publisher esperado.

### 6. Tests de entorno

```powershell
dotnet test middleware\Trazzo.Middleware.slnx
```

La suite corre en serie (`DisableTestParallelization`): un gate estatico compartido (`BiometricSecurityGates.AllowPlaintextTemplateFallback`) hacia fallar tests en paralelo.

Los tests de `LocalWebSocketServerServiceTests` levantan `HttpListener` y pueden fallar en entornos restringidos con:

```text
System.Net.HttpListenerException: Controlador no valido.
```

Eso es restriccion del entorno de Windows/runner, no necesariamente bug funcional del middleware. Validar esos tests en una maquina Windows con permisos normales para abrir listener local.

## MSI

El build Release genera:

```text
Trazzo.Biometric.Agent.Installer\bin\Release\es-ES\Trazzo.Biometric.Agent.msi
Trazzo.Biometric.Agent.Installer\bin\Release\en-US\Trazzo.Biometric.Agent.msi
```

Comando:

```powershell
dotnet build .\Trazzo.Biometric.Agent.Installer\Trazzo.Biometric.Agent.Installer.wixproj -c Release
```
