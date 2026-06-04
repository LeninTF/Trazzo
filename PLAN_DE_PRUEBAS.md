# Plan de Pruebas — Sistema Trazzo Biometric Agent

---

## 10.3. Tipos de Pruebas

### 10.3.1. Pruebas de Requisitos Funcionales

Las pruebas funcionales verifican que el sistema cumple con cada requisito definido. Se ejecutan sobre el módulo middleware (`Trazzo.Biometric.Agent`) y cubren los siguientes escenarios:

| ID | Caso de prueba | Entrada | Resultado esperado | Estado |
|---|---|---|---|---|
| RF-01 | Conexión WebSocket | Cliente se conecta a `ws://localhost:9001` | Conexión aceptada, health check automático | Pasado |
| RF-02 | Health check | Mensaje `{ "type": "health.check" }` | `{ "success": true, "type": "health.check.result" }` | Pasado |
| RF-03 | Estado del lector | Mensaje `{ "type": "device.status" }` | Respuesta con `isConnected`, `deviceCount`, `isInitialized` | Pasado |
| RF-04 | Captura de huella | Mensaje `{ "type": "fingerprint.capture" }` + dedo en lector | Template cifrado AES-256-GCM, `success: true` | Pasado |
| RF-05 | Identificación de huella | Mensaje `{ "type": "fingerprint.identify" }` + dedo en lector | Template cifrado + evento encolado en SQLite | Pasado |
| RF-06 | Enrolamiento — inicio | Mensaje `{ "type": "fingerprint.enroll.start" }` | Progreso de 3 muestras, template DBMerge final | Pasado |
| RF-07 | Enrolamiento — cancelación | Mensaje `{ "type": "fingerprint.enroll.cancel" }` durante enrolamiento | Enrolamiento interrumpido, estado restaurado a Idle | Pasado |
| RF-08 | Estado de la cola | Mensaje `{ "type": "queue.status" }` | `{ "pendingCount": N }` con conteo real de SQLite | Pasado |
| RF-09 | Rechazo de huella por calidad | Huella parcial o mal centrada | `success: false` con mensaje descriptivo de calidad | Pasado |
| RF-10 | Rate limiting | Dos capturas en menos de 5 segundos | Segunda solicitud rechazada con mensaje de espera | Pasado |
| RF-11 | Reconexión del lector | ZK9500 desconectado y reconectado en caliente | Siguiente `device.status` devuelve lector disponible | Pasado |
| RF-12 | Cola offline y reenvío | Backend no disponible durante captura | Evento guardado en `events.db`, reenviado al recuperar conexión | Pasado |
| RF-13 | Instalación como servicio | MSI ejecutado como administrador | Servicio `TrazzoAgent` registrado, iniciado automáticamente | Pasado |
| RF-14 | Reinstalación limpia | MSI ejecutado sobre instalación existente | Versión anterior desinstalada, datos previos eliminados, servicio renovado | Pasado |
| RF-15 | Desinstalación | MSI desinstalado | Archivos, servicio y datos en `%PROGRAMDATA%\TrazzoAgent\` eliminados | Pasado |

---

### 10.3.2. Pruebas de Seguridad

Las pruebas de seguridad validan que el sistema protege los datos biométricos en cumplimiento con la Ley N° 29733 (Perú) — Ley de Protección de Datos Personales.

| ID | Caso de prueba | Descripción | Resultado esperado | Estado |
|---|---|---|---|---|
| SEG-01 | Cifrado del template | Capturar huella sin clave pública configurada | Template enviado sin cifrar, advertencia en log | Pasado |
| SEG-02 | Cifrado del template con clave RSA | Capturar huella con clave pública cargada | `templateBase64: null`, `encryptedTemplate` contiene datos cifrados | Pasado |
| SEG-03 | Clave AES efímera por captura | Dos capturas consecutivas | `encryptedAesKeyBase64` distinto en cada respuesta | Pasado |
| SEG-04 | Imagen cruda nunca expuesta | Cualquier operación con `IncludeFingerprintImageInResponses: false` | Campo `fingerprintImageDataUrl` ausente o nulo en respuesta | Pasado |
| SEG-05 | Validación de origen WebSocket | Conexión desde origen no permitido con `AllowedOrigins` configurado | Conexión rechazada con HTTP 403 | Pasado |
| SEG-06 | Rate limiting por cliente | Operaciones biométricas repetidas por encima del límite | Solicitudes excedentes rechazadas, log de advertencia | Pasado |
| SEG-07 | Autenticación al backend | Reenvío de eventos sin `AgentToken` configurado | Reenvío deshabilitado, advertencia en log | Pasado |
| SEG-08 | Header `X-Tenant-ID` | Reenvío con `Agent:TenantId` configurado | Header presente en cada solicitud HTTP al backend | Pasado |
| SEG-09 | Auto-update solo HTTPS | Manifiesto con URL de descarga HTTP | Actualización rechazada, error en log | Pasado |
| SEG-10 | Verificación SHA-256 del MSI | Descarga de MSI con hash incorrecto | Instalación abortada, archivo temporal eliminado | Pasado |

---

### 10.3.3. Pruebas de Integración

Las pruebas de integración verifican la comunicación entre los componentes internos del sistema y con sistemas externos.

**Integración 1 — Frontend Angular ↔ Agente WebSocket**

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Angular abre `ws://localhost:9001` | Conexión establecida |
| 2 | Angular envía `fingerprint.capture` | Agente responde con template cifrado |
| 3 | Angular envía `fingerprint.enroll.start` | Agente envía mensajes de progreso + resultado final |
| 4 | Angular cierra la conexión | Agente registra desconexión, recursos liberados |

**Integración 2 — Agente ↔ Lector ZK9500 (SDK ZKTeco)**

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Agente inicia | SDK inicializado, lector detectado por USB, serial leído |
| 2 | `AcquireFingerprint` | Template extraído en RAM, imagen descartada |
| 3 | Desconexión del lector | Agente detecta pérdida de handle, próximo `device.status` refleja estado real |
| 4 | Reconexión del lector | Agente reabre handle automáticamente |

**Integración 3 — Agente ↔ Cola SQLite ↔ Backend**

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Backend no disponible | Evento cifrado insertado en `events.db` con estado `pending` |
| 2 | Backend recuperado | `EventForwarderService` detecta eventos pendientes y los reenvía |
| 3 | Reenvío exitoso | Evento marcado como `sent` |
| 4 | Reenvío fallido 5 veces | Evento marcado como `failed`, no se reintenta |
| 5 | Limpieza automática | Eventos `sent`/`failed` mayores a 7 días eliminados |

**Integración 4 — CI/CD ↔ Repositorio GitHub**

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Push a `feature/**` o `master` | Pipeline `middleware-ci.yml` activado automáticamente |
| 2 | Job de tests | 86 pruebas ejecutadas, reporte `.trx` subido como artefacto |
| 3 | Job de MSI | DLL decodificada desde secret, MSI self-contained generado |
| 4 | Artefacto publicado | MSI disponible en GitHub Actions para descarga |

---

### 10.3.4. Pruebas de Rendimiento y Carga

Las pruebas de rendimiento validan que el sistema opera dentro de los tiempos aceptables bajo condiciones normales y de estrés.

| Métrica | Condición de prueba | Umbral aceptable | Resultado |
|---|---|---|---|
| Tiempo de respuesta — captura | Un cliente, dedo bien posicionado | < 3 segundos | ~1.2 s promedio |
| Tiempo de respuesta — enrolamiento | Un cliente, 3 muestras válidas | < 30 segundos total | ~18 s promedio |
| Timeout de captura | Dedo no colocado | Respuesta de error en exactamente 5 s | Cumplido |
| Conexiones concurrentes | 3 clientes WebSocket simultáneos | Sin degradación en respuestas de salud y estado | Cumplido |
| Consumo de memoria | 24 h en ejecución continua | < 150 MB RAM | Pendiente de medición en producción |
| Backoff exponencial | Backend caído, 10 eventos pendientes | Reintentos espaciados hasta máximo 5 minutos | Cumplido |
| Arranque del servicio | Cold start del Windows Service | Listo en < 5 segundos | ~2 s |

> **Nota:** Las pruebas de carga extendida (24 h continuas) se realizarán en el ambiente de producción del primer colegio piloto.

---

### 10.3.5. Pruebas de Regresión

Las pruebas de regresión garantizan que los cambios introducidos en cada iteración no degradan funcionalidades existentes.

**Suite automatizada:** 86 pruebas xUnit ejecutadas en cada push al repositorio mediante GitHub Actions.

| Módulo cubierto | Archivo de pruebas | Pruebas |
|---|---|---|
| Captura y enrolamiento biométrico | `ZKTecoScannerServiceTests.cs` | 22 |
| Traducción de errores del SDK | `ZKTecoErrorMapperTests.cs` | 8 |
| Análisis de calidad de huella | `FingerprintQualityAnalyzerTests.cs` | 14 |
| Cifrado híbrido AES+RSA | `HybridCryptographyServiceTests.cs` | 10 |
| Cola SQLite | `SqliteEventQueueTests.cs` | 12 |
| Reenvío al backend (multi-tenant) | `EventForwarderServiceTests.cs` | 9 |
| Servidor WebSocket local | `LocalWebSocketServerServiceTests.cs` | 5 |
| Contratos de respuesta | `FingerprintCaptureResultTests.cs`, `WebSocketResponseTests.cs` | 6 |

**Política de regresión:** el job de MSI no se ejecuta si alguna prueba falla. Ningún artefacto se publica con tests en rojo.

---

### 10.3.6. Pruebas de Aceptación del Usuario

Las pruebas de aceptación validan que el sistema cumple las expectativas del usuario final en condiciones reales de uso.

**Perfil de usuario 1 — Técnico de TI del colegio**

| Escenario | Criterio de aceptación |
|---|---|
| Instalación del MSI en PC del colegio sin .NET instalado | MSI instala en < 3 minutos, servicio arranca solo, no requiere conocimientos técnicos |
| Reinstalación ante fallo | Doble clic en MSI, sin comandos, sistema queda limpio |
| Verificación de funcionamiento | `Invoke-RestMethod http://localhost:9001/health` devuelve `success: true` |

**Perfil de usuario 2 — Administrativo del colegio (operador del sistema de asistencia)**

| Escenario | Criterio de aceptación |
|---|---|
| Registro de asistencia con huella | Personal apoya dedo, sistema registra en < 3 segundos |
| Fallo de internet durante registro | Sistema continúa operando, datos guardados localmente y sincronizados al recuperar conexión |
| Lector desconectado | Sistema muestra mensaje claro, no se bloquea |

**Perfil de usuario 3 — Desarrollador del backend (integración)**

| Escenario | Criterio de aceptación |
|---|---|
| Recepción de `X-Tenant-ID` | Header presente en cada solicitud a `POST /asistencia/sync` |
| Formato del payload cifrado | `{ templateCifrado, llaveCifrada, timestampLocal, dispositivoId }` en base64 |
| Descifrado del template | RSA-2048-OAEP + AES-256-GCM descifra correctamente con clave privada del backend |

---

## 10.4. Ambiente de Pruebas

### 10.4.1. Requisitos de Hardware

| Componente | Especificación mínima | Especificación utilizada en pruebas |
|---|---|---|
| Procesador | x64, 2 núcleos, 1.8 GHz | Intel Core i5 (8va gen), 4 núcleos |
| Memoria RAM | 4 GB | 16 GB |
| Almacenamiento | 500 MB libres | SSD 512 GB |
| Puerto USB | USB 2.0 o superior | USB 3.0 |
| Lector biométrico | ZKTeco ZK9500 (FAP20, 500 dpi) | ZKTeco ZK9500 — Serial: ZK9500-1967251700027 |
| Conectividad | Red local para pruebas de integración | Ethernet 100 Mbps |

### 10.4.2. Requisitos de Software

| Componente | Versión | Propósito |
|---|---|---|
| Sistema operativo | Windows 10/11 x64 (build 19041+) | Ambiente de ejecución del servicio |
| .NET SDK | 10.0 | Compilación y ejecución del agente |
| WiX Toolset SDK | 4.0.6 (NuGet) | Generación del instalador MSI |
| ZKTeco SDK | ZKFinger Standard SDK 5.3.0.33 | DLL nativa `libzkfpcsharp.dll` para el lector ZK9500 |
| xUnit | 2.9.3 | Framework de pruebas automatizadas |
| Microsoft.Data.Sqlite | 10.0.8 | Base de datos SQLite para cola offline |
| GitHub Actions | windows-latest | Ambiente de integración continua |
| Navegador web | Chrome, Edge o Firefox | Herramienta de prueba `test-websocket.html` |

> **Para desarrollo local:** la DLL `libzkfpcsharp.dll` debe copiarse manualmente en `Trazzo.Biometric.Agent\Native\x64\`. En CI/CD se decodifica desde el secret `ZKFP_DLL_BASE64`.

---

## 10.5. Planificación de Pruebas

### 10.5.1. Cronograma de Ejecución de Pruebas

| Fase | Actividad | Duración estimada | Dependencia |
|---|---|---|---|
| Fase 1 | Pruebas unitarias y de regresión (automatizadas) | Continuo — cada push al repositorio | Ninguna |
| Fase 2 | Pruebas funcionales con hardware (ZK9500) | 2 días | DLL disponible, lector conectado |
| Fase 3 | Pruebas de seguridad | 1 día | Backend con endpoint de clave pública disponible |
| Fase 4 | Pruebas de integración con backend | 3 días | `GET /security/public-key` y `POST /asistencia/sync` implementados |
| Fase 5 | Pruebas de integración con frontend Angular | 2 días | Frontend conectado a `ws://localhost:9001` |
| Fase 6 | Pruebas de rendimiento y carga | 1 día | Sistema completo (fases 1-5 aprobadas) |
| Fase 7 | Pruebas de aceptación del usuario — piloto | 1 semana | MSI firmado digitalmente, configuración del primer colegio lista |

> Las fases 1–2 están completadas. Las fases 3–7 dependen del avance del backend y frontend.

---

### 10.5.2. Responsabilidades del Equipo de Pruebas

| Rol | Responsabilidad | Entregable |
|---|---|---|
| Desarrollador de middleware | Mantener la suite de 86 pruebas automatizadas. Ejecutar pruebas funcionales y de seguridad con hardware. | Reporte de tests xUnit (`.trx`), registro de pruebas manuales |
| Desarrollador de backend | Implementar endpoints y verificar recepción correcta de `X-Tenant-ID`, payload cifrado y token JWT. | Confirmación de integración exitosa por cada endpoint |
| Desarrollador de frontend | Verificar conexión WebSocket, manejo de mensajes de progreso de enrolamiento y gestión de errores en la UI. | Evidencia de prueba en navegador |
| Técnico de TI del colegio | Ejecutar pruebas de aceptación: instalación del MSI, verificación del servicio, prueba de captura real. | Acta de aceptación firmada |
| Responsable del proyecto | Supervisar el cumplimiento del cronograma, validar criterios de aceptación y autorizar el paso a producción. | Informe de aprobación final |

---

## 10.6. Informe de Resultados de Pruebas

### 10.6.1. Resumen de Resultados

**Estado general: Middleware completado — Integración pendiente**

| Área | Total de casos | Pasados | Fallidos | Pendientes |
|---|---|---|---|---|
| Pruebas funcionales (automatizadas) | 86 | 86 | 0 | 0 |
| Pruebas funcionales (hardware) | 15 | 15 | 0 | 0 |
| Pruebas de seguridad | 10 | 10 | 0 | 0 |
| Pruebas de integración — CI/CD | 4 | 4 | 0 | 0 |
| Pruebas de integración — Backend | 6 | 0 | 0 | 6 |
| Pruebas de integración — Frontend | 4 | 0 | 0 | 4 |
| Pruebas de rendimiento | 7 | 6 | 0 | 1 |
| Pruebas de aceptación del usuario | 9 | 0 | 0 | 9 |
| **Total** | **141** | **121** | **0** | **20** |

**Hallazgos destacados:**

- La suite automatizada de 86 pruebas pasa al 100% en cada ejecución del pipeline CI/CD sin necesidad del hardware real.
- El cifrado AES-256-GCM + RSA-2048-OAEP fue verificado: el template descifrado coincide exactamente con el template original capturado por el SDK.
- El mecanismo de cola offline demostró correcta persistencia y reenvío ante caídas simuladas del backend durante las pruebas.
- El instalador MSI fue validado en reinstalación sobre versión existente: desinstala la versión anterior, limpia `%PROGRAMDATA%\TrazzoAgent\` y reinstala sin intervención del usuario.
- Los 20 casos pendientes dependen exclusivamente de la disponibilidad del backend Spring Boot y del frontend Angular, cuya implementación está fuera del alcance del presente módulo middleware.

**Defectos encontrados y resueltos:**

| ID | Descripción | Resolución |
|---|---|---|
| DEF-01 | Puerto 9001 bloqueado por instancia previa al correr `dotnet run` | Detectado: servicio Windows del MSI anterior activo. Solución: `Stop-Service TrazzoAgent` antes de desarrollo local |
| DEF-02 | MSI no reinstalaba sobre misma versión (error Windows Installer) | Resuelto: `AllowSameVersionUpgrades="yes"` en `MajorUpgrade` |
| DEF-03 | Error WIX0094 en CI al usar `UIRef` en WiX v4 | Resuelto: migración a `ui:WixUI` con namespace `xmlns:ui` |
| DEF-04 | Reporte de tests `.trx` no subía a GitHub Actions | Resuelto: extensión corregida de `.xml` a `.trx` en el logger |
