# Trazzo Biometric Agent

Middleware local de Windows que conecta el lector ZKTeco ZK9500 con el frontend de Trazzo. Corre como un servicio en segundo plano y expone un WebSocket local en `ws://localhost:9001/` al que el navegador se conecta directamente.

El flujo básico es simple: el frontend pide una captura, el agente habla con el lector por USB, obtiene la plantilla biométrica, la cifra con AES-256-GCM usando la llave pública del backend Spring Boot, y devuelve el resultado cifrado. Si el backend no está disponible, el evento queda en cola local y se reenvía automáticamente cuando vuelve la conexión.

---

## Requisitos

- Windows x64
- .NET SDK 10
- ZKTeco ZK9500 conectado y visible en el Administrador de dispositivos
- DLL oficial del SDK ZKTeco (`libzkfpcsharp.dll`, versión x64)

---

## Configuración inicial

### 1. Copiar la DLL del SDK

Copia la DLL x64 del SDK oficial de ZKTeco en:

```
Native\x64\libzkfpcsharp.dll
```

Esta DLL no está en el repositorio porque tiene licencia privada de ZKTeco. Si falta o no puede cargarse, el agente lo indica en los logs y en la respuesta de estado.

### 2. Configurar el backend

Abre `appsettings.json` y completa las URLs del backend Spring Boot:

```json
{
  "Security": {
    "BackendPublicKeyUrl": "https://api.trazzo.com/api/security/public-key"
  },
  "Queue": {
    "BackendUrl": "https://api.trazzo.com/api/asistencias/sync",
    "RetryIntervalSeconds": 30
  }
}
```

- **`BackendPublicKeyUrl`**: endpoint desde donde el agente descarga la llave pública RSA al arrancar. Se usa para cifrar los templates biométricos antes de enviarlos.
- **`BackendUrl`**: endpoint al que el agente reenvía automáticamente los eventos que quedaron en cola mientras el backend no estaba disponible.

Si dejas estas URLs vacías, el agente funciona en modo desarrollo: los templates viajan en Base64 plano y no se activa la cola de reenvío. Nunca uses ese modo en producción.

---

## Ejecutar

Antes de iniciar, cierra cualquier proceso de ZKTeco que pueda tener el lector ocupado:

```powershell
taskkill /F /IM ISSOnline_App.exe
taskkill /F /IM ISSOnline.exe
taskkill /F /IM iZHost.exe
taskkill /F /IM ZKOnlineProtect.exe
```

Verifica que Windows detecta el lector:

```powershell
Get-PnpDevice -PresentOnly | Where-Object {$_.FriendlyName -match 'ZK9500'} | Format-Table Status,Class,FriendlyName -AutoSize
```

Compila y ejecuta:

```powershell
dotnet build
dotnet run
```

Al arrancar deberías ver algo así en la consola:

```
Agente biométrico de Trazzo iniciado.
WebSocket escuchando en ws://localhost:9001/
Clave pública RSA-2048 actualizada. Cifrado AES-256-GCM + RSA-2048-OAEP habilitado.
SDK inicializado.
Cantidad de lectores: 1.
Lector abierto en el índice 0.
```

Si la URL de la llave pública no está configurada o el backend no responde todavía, verás una advertencia pero el agente sigue funcionando en modo sin cifrado.

---

## Probar con el WebSocket

La forma más rápida de probar es abrir el archivo incluido:

```
scripts/test-websocket.html
```

Tiene botones para cada operación y muestra las respuestas en pantalla.

También puedes probarlo desde la consola del navegador:

```javascript
const ws = new WebSocket("ws://localhost:9001");

ws.onopen = () => ws.send(JSON.stringify({ type: "health.check" }));
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

---

## Mensajes soportados

### `health.check`

Confirma que el agente está corriendo.

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

### `device.status`

Hace una verificación viva contra el SDK. No usa estado cacheado.

```json
{ "type": "device.status" }
```

```json
{
  "type": "device.status.result",
  "success": true,
  "message": "Lector biométrico encontrado.",
  "deviceCount": 1,
  "isSdkAvailable": true,
  "isInitialized": true,
  "isDeviceOpen": true,
  "isConnected": true
}
```

Cuando el lector está desconectado:

```json
{
  "type": "device.status.result",
  "success": false,
  "message": "Lector biométrico desconectado.",
  "deviceCount": 0,
  "isDeviceOpen": false,
  "isConnected": false
}
```

### `fingerprint.capture`

Captura simple. Útil para pruebas manuales o cuando el frontend solo necesita el template sin identificar.

```json
{ "type": "fingerprint.capture" }
```

Respuesta cuando el cifrado está configurado:

```json
{
  "type": "fingerprint.capture.result",
  "success": true,
  "message": "Huella capturada correctamente.",
  "templateBase64": null,
  "encryptedTemplate": {
    "encryptedTemplateBase64": "...",
    "encryptedAesKeyBase64": "...",
    "ivBase64": "...",
    "tagBase64": "..."
  },
  "templateSize": 1186,
  "deviceId": "ZK9500-12345",
  "capturedAtUtc": "2026-05-27T00:00:00Z"
}
```

En modo desarrollo (sin llave RSA configurada), `encryptedTemplate` es `null` y `templateBase64` contiene el template en Base64 plano.

### `fingerprint.identify`

Captura temporal para marcar asistencia. Una sola lectura, sin DBMerge, sin guardar nada localmente.

```json
{ "type": "fingerprint.identify" }
```

La respuesta tiene la misma estructura que `fingerprint.capture.result` pero con `type: "fingerprint.identify.result"`.

### `fingerprint.enroll.start`

Inicia el enrolamiento de un empleado nuevo. Requiere tres capturas válidas. El agente envía progreso entre cada paso:

```json
{ "type": "fingerprint.enroll.start" }
```

Progreso intermedio (llega uno por cada paso):

```json
{
  "type": "fingerprint.enroll.progress",
  "step": 1,
  "totalSteps": 3,
  "message": "Muestra 1 de 3 capturada. Retire el dedo."
}
```

Resultado final exitoso:

```json
{
  "type": "fingerprint.enroll.result",
  "success": true,
  "message": "Huella enrolada correctamente.",
  "registeredTemplateBase64": null,
  "encryptedRegisteredTemplate": {
    "encryptedTemplateBase64": "...",
    "encryptedAesKeyBase64": "...",
    "ivBase64": "...",
    "tagBase64": "..."
  },
  "registeredTemplateSize": 1186,
  "capturedSamples": 3,
  "deviceId": "ZK9500-12345",
  "capturedAtUtc": "2026-05-27T00:00:00Z"
}
```

El backend de Trazzo guarda la plantilla cifrada y la asocia al trabajador. Los tres templates individuales nunca se devuelven ni se persisten.

### `fingerprint.enroll.cancel`

Cancela un enrolamiento activo.

```json
{ "type": "fingerprint.enroll.cancel" }
```

### `queue.status`

Consulta cuántos eventos biométricos están pendientes de reenvío al backend.

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

Útil para mostrar en el frontend un indicador de sincronización pendiente, por ejemplo cuando hubo corte de internet.

---

## Cifrado de templates

El agente implementa cifrado híbrido para proteger los datos biométricos en tránsito:

1. Al arrancar, descarga la llave pública RSA-2048 del backend desde `Security:BackendPublicKeyUrl`.
2. Si el endpoint no responde (sin internet, backend caído), usa una copia cacheada en disco en `%PROGRAMDATA%\TrazzoAgent\public_key.cache`.
3. Si tampoco hay caché, arranca sin cifrado y lo indica en los logs.
4. Cada 24 horas refresca la llave en segundo plano sin reiniciar el servicio.

Para cada captura exitosa:

- Genera una llave AES-256 efímera y un nonce de 96 bits aleatorios.
- Cifra el template con AES-256-GCM.
- Cifra la llave AES con RSA-2048-OAEP usando la llave pública del backend.
- Devuelve los cuatro campos (`encryptedTemplateBase64`, `encryptedAesKeyBase64`, `ivBase64`, `tagBase64`).

El backend descifra en orden inverso: primero la llave AES con su llave privada RSA, luego el template con AES-GCM verificando el tag de autenticación.

El endpoint que expone Spring Boot:

```
GET /api/security/public-key
```

Respuesta:

```json
{
  "publicKey": "<SubjectPublicKeyInfo en Base64>",
  "kid": "<identificador de la clave>"
}
```

El agente usa `publicKey` e ignora `kid`.

---

## Identificación del lector (DeviceId)

Todas las respuestas biométricas incluyen el campo `deviceId` con el número de serie del ZK9500 leído directamente del hardware:

```json
"deviceId": "ZK9500-12345"
```

Este campo permite al backend verificar que la captura proviene de un lector registrado y no de una fuente externa, lo que es parte de la protección anti-spoofing.

---

## Cola offline (Store & Forward)

Cada vez que el agente realiza una captura o enrolamiento exitoso con cifrado activo, guarda automáticamente el evento en una base de datos SQLite local en:

```
%PROGRAMDATA%\TrazzoAgent\events.db
```

Un proceso en segundo plano revisa la cola cada `Queue:RetryIntervalSeconds` segundos e intenta reenviar los eventos al backend. Cuando el POST es exitoso, marca el evento como enviado. Después de 5 intentos fallidos, el evento queda en estado `failed` y deja de reintentarse.

Los eventos enviados con más de 7 días de antigüedad se eliminan automáticamente para no crecer indefinitely.

Este mecanismo garantiza que no se pierda ningún registro de asistencia aunque el internet se corte durante la jornada. El frontend puede consultar cuántos registros están pendientes con `queue.status` y mostrar un indicador visual al usuario.

El endpoint de Spring Boot que recibe estos eventos:

```
POST /api/asistencias/sync
```

Body enviado:

```json
{
  "templateCifrado": "<base64 de IV[12 bytes] + Tag[16 bytes] + Ciphertext>",
  "llaveCifrada": "<base64 de la llave AES cifrada con RSA-2048-OAEP>",
  "timestampLocal": "2026-05-27T00:00:00+00:00",
  "dispositivoId": "ZK9500-12345"
}
```

El campo `templateCifrado` empaqueta IV, tag de autenticación y ciphertext en un solo Base64 concatenado. El backend extrae los primeros 12 bytes como IV, los siguientes 16 como tag AES-GCM, y el resto como ciphertext.

---

## Validación de calidad

El SDK puede generar una plantilla aunque la huella esté incompleta. Por eso el agente aplica su propia validación sobre la imagen en escala de grises:

- Tamaño mínimo de plantilla (bytes)
- Porcentaje de área oscura detectada como huella (cobertura)
- Contraste entre el fondo y la huella
- Posición centrada del dedo en el sensor

Si no pasa la validación, la respuesta devuelve `success: false` y el campo `quality` con el detalle:

```json
{
  "type": "fingerprint.capture.result",
  "success": false,
  "message": "Huella incompleta o mal posicionada. Coloque el dedo completo y centrado sobre el lector.",
  "quality": {
    "isAcceptable": false,
    "foregroundCoveragePercent": 7.4,
    "contrastScore": 31.2,
    "isCentered": false,
    "message": "Área de huella insuficiente."
  }
}
```

Configuración recomendada para asistencia real:

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

Ajusta estos valores con pruebas reales porque la presión del dedo, la iluminación interna del sensor y el estado del cristal pueden variar entre equipos.

---

## Control de sesión

El agente solo acepta lecturas dentro de una operación iniciada por WebSocket. Si no hay operación activa, cualquier dedo colocado en el lector se ignora.

Estados internos:

| Estado | Significado |
|--------|-------------|
| `Idle` | Listo para recibir una operación |
| `Capturing` | Captura simple en progreso |
| `Identifying` | Identificación en progreso |
| `Enrolling` | Enrolamiento en progreso |
| `Cooldown` | Pausa corta después de una operación |

Solo se permite iniciar si el estado es `Idle`. Si el usuario manda varias solicitudes seguidas, el agente responde `Ya hay una operación biométrica en progreso.`

Después de cada resultado (éxito, timeout, error o cancelación), el agente limpia el estado, drena lecturas residuales y queda en `Cooldown` brevemente antes de volver a `Idle`.

Si `RequireFingerLiftBeforeNextCapture` está en `true`, el agente verifica que el dedo no esté apoyado antes de iniciar una operación nueva. Si detecta un dedo pegado, responde `Retire el dedo del lector y vuelva a colocarlo.`

---

## Reconexión del lector

Si el ZK9500 se desconecta físicamente durante una captura, la sesión se cancela y el agente responde con el error correspondiente. El SDK y los handles se limpian internamente.

Cuando el lector se vuelve a conectar, el mensaje `device.status` hace una verificación viva: detecta el dispositivo, abre el handle y reasigna los buffers sin necesidad de reiniciar `dotnet run`.

---

## Imagen de huella (solo pruebas locales)

Por defecto `IncludeFingerprintImageInResponses` está en `false` y el agente nunca devuelve la imagen raw de la huella. Esto es obligatorio en producción.

Para desarrollo local puedes activarlo temporalmente:

```json
{
  "Biometric": {
    "IncludeFingerprintImageInResponses": true
  }
}
```

Cuando está activo, la respuesta incluye:

```json
{
  "fingerprintImageBase64": "...",
  "fingerprintImageMimeType": "image/png",
  "fingerprintImageDataUrl": "data:image/png;base64,..."
}
```

El archivo `scripts/test-websocket.html` muestra esta imagen en pantalla cuando está presente.

---

## Errores comunes

**`No se encontró la DLL del SDK ZKTeco o no se pudo cargar.`**
Verifica que `Native\x64\libzkfpcsharp.dll` existe y es la versión x64 del SDK oficial.

**`No se encontró ningún lector biométrico.`**
El ZK9500 no está conectado, Windows no lo detecta, o un proceso de ZKTeco ya tiene el lector ocupado. Cierra `ISSOnline_App.exe`, `iZHost.exe` y similares.

**`Tiempo de espera agotado. Coloque el dedo en el lector.`**
El usuario no puso el dedo dentro del tiempo máximo configurado (`CaptureTimeoutSeconds`).

**`Ya hay una captura de huella en progreso.`**
Espera a que termine la operación actual antes de enviar otra solicitud.

**El agente arranca sin cifrado activado.**
Verifica que `Security:BackendPublicKeyUrl` está configurada y que el endpoint de Spring Boot responde correctamente. El agente intenta reconectarse al refrescar la llave cada 24 horas, pero también puedes reiniciarlo para que lo intente de inmediato.

---

## Publicar como servicio de Windows

Una vez validado con `dotnet run`, publica y registra el servicio:

```powershell
dotnet publish -c Release -r win-x64 --self-contained false -o publish\

sc create "TrazzoAgent" binPath="C:\ruta\a\publish\Trazzo.Biometric.Agent.exe" start=auto
sc start "TrazzoAgent"
```

Para detenerlo o eliminarlo:

```powershell
sc stop "TrazzoAgent"
sc delete "TrazzoAgent"
```

---

## Configuración completa de referencia

```json
{
  "Agent": {
    "WebSocketUrl": "http://localhost:9001/"
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
      "CenterTolerancePercent": 28
    }
  },
  "Enrollment": {
    "RequiredSamples": 3,
    "SampleTimeoutSeconds": 8,
    "RequireFingerLiftBetweenSamples": true
  },
  "Security": {
    "BackendPublicKeyUrl": "https://api.trazzo.com/api/security/public-key"
  },
  "Queue": {
    "BackendUrl": "https://api.trazzo.com/api/asistencias/sync",
    "RetryIntervalSeconds": 30
  }
}
```

---

## Pendiente

Estas cosas están diseñadas pero aún no implementadas:

**Instalador MSI**
Empaquetado como instalador Windows (.msi) que registra el servicio automáticamente, copia la DLL del SDK y configura los permisos necesarios. Actualmente hay que hacer `sc create` a mano.

**Auto-updater silencioso**
El agente debería verificar periódicamente si hay una versión nueva disponible y actualizarse sin intervención del usuario. Por ahora hay que hacer `dotnet publish` y reemplazar el ejecutable a mano.

