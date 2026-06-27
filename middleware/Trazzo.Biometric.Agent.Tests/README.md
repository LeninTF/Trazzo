# Trazzo Biometric Agent — Tests

Suite de pruebas xUnit para el **Trazzo Biometric Agent**. Cubre toda la lógica de negocio sin necesitar hardware real ni base de datos externa.

**218 pruebas** — todas unitarias o de integración in-process. No requieren el lector ZK9500, la DLL nativa ni conexión al backend.

---

## Ejecutar los Tests

```powershell
dotnet test
```

O desde la raíz del middleware:

```powershell
dotnet test .\Trazzo.Biometric.Agent.Tests\Trazzo.Biometric.Agent.Tests.csproj
```

---

## Estructura de Archivos

| Archivo | Qué prueba |
|---|---|
| `ZKTecoScannerServiceTests.cs` | Captura, identificación y enrolamiento biométrico con fake SDK |
| `ZKTecoErrorMapperTests.cs` | Traducción de códigos de error del SDK a mensajes legibles |
| `FingerprintQualityAnalyzerTests.cs` | Análisis de calidad de huella: cobertura, contraste, centrado, tamaño |
| `HybridCryptographyServiceTests.cs` | Cifrado AES-256-GCM + RSA-2048-OAEP, caché de clave pública |
| `SqliteEventQueueTests.cs` | Cola SQLite: encolar, obtener pendientes, marcar enviados/fallidos, limpieza |
| `EventForwarderServiceTests.cs` | Reenvío al backend, cabecera `X-Tenant-ID`, backoff exponencial, multi-tenant |
| `LocalWebSocketServerServiceTests.cs` | Manejo de mensajes WebSocket, rate limiting, tipos de operación |
| `FingerprintCaptureResultTests.cs` | Serialización y estructura de respuestas de captura |
| `FingerprintImageConverterTests.cs` | Conversión de imagen de huella a Base64 |
| `WebSocketResponseTests.cs` | Estructura de respuestas JSON del protocolo WebSocket |
| `Fakes.cs` | Implementaciones falsas compartidas: `FakeBiometricScanner`, `FakeEventQueue`, etc. |

---

## Fakes — Sin Hardware

Todos los tests usan implementaciones falsas definidas en `Fakes.cs`:

- **`FakeZKTecoSdk`** — simula el SDK nativo del ZK9500 sin DLL ni lector
- **`FakeBiometricScannerService`** — devuelve resultados configurables de captura/enrolamiento
- **`FakeEventQueue`** — cola en memoria para verificar eventos encolados
- **`FakeHybridCryptography`** — cifrado simulado para aislar tests de la lógica criptográfica

---

## Tecnologías

- **xUnit** 2.9 — framework de pruebas
- **Microsoft.Data.Sqlite** — SQLite real en memoria para `SqliteEventQueueTests`
- **SourceGear.sqlite3** 3.50.4.5 — reemplaza `e_sqlite3.dll` de `SQLitePCLRaw.lib.e_sqlite3` con SQLite 3.50.4 parcheado (GHSA-2m69-gcr7-jv3q)
- **coverlet** — cobertura de código

---

## Cobertura Notable

### Multi-tenant (`EventForwarderServiceTests`)

Verifica que el header `X-Tenant-ID` se envía correctamente cuando `Agent:TenantId` está configurado, y que se omite cuando está vacío.

### Calidad de huella (`FingerprintQualityAnalyzerTests`)

Prueba cada criterio de calidad individualmente y en combinación: cobertura mínima/máxima de primer plano, contraste, centrado y tamaño mínimo de template.

### Cola offline (`SqliteEventQueueTests`)

Prueba el ciclo completo de un evento: encolar → pendiente → enviado/fallido → limpieza por antigüedad. Usa SQLite real en memoria, no mocks.

### Cifrado (`HybridCryptographyServiceTests`)

Verifica que el template cifrado difiere del original, que la clave AES nunca se transmite en claro, y que el caché de clave pública se reutiliza entre capturas.
