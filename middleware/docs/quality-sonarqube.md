# Guía de Calidad — SonarCloud para Middleware Biométrico

**Proyecto SonarCloud:** `methodsync-trazzo-middleware`  
**Organización:** `methodsync`  
**Última actualización:** 2026-06-10

---

## Resumen de cambios aplicados

### Problemas resueltos

| Regla | Archivo | Descripción | Fix aplicado |
|-------|---------|-------------|--------------|
| S6966 | `AutoUpdate/AutoUpdateService.cs` | Fire-and-forget task en timer callback | Reescrito con constructor interno; timer eliminado del flujo |
| S6966 | `Security/HybridCryptographyService.cs` | Async void en callback del `Timer` | `ContinueWith(OnlyOnFaulted, TaskScheduler.Default)` |
| S6966 | `WebSocket/LocalWebSocketServerService.cs` | `_ = Task.Run(...)` sin manejo de errores | Extraído a método `StartContextHandler` con `ContinueWith` |
| S6966 | `ZKTeco/ZKTecoScannerService.cs` | Fire-and-forget en `FinalizeOperation` | `ContinueWith(OnlyOnFaulted, TaskScheduler.Default)` |
| S5332 | `AutoUpdate/AutoUpdateService.cs` | Uso potencial de HTTP para descarga | Validación `Uri.UriSchemeHttps` bloqueante en `ExecuteAsync` y `CheckAndApplyUpdateAsync` |
| S5332 | `Security/HybridCryptographyService.cs` | Uso potencial de HTTP para clave pública | Validación `Uri.UriSchemeHttps` en `BuildHttpFetcher` |
| S4721 | `AutoUpdate/AutoUpdateService.cs` | `Process.Start` sin validación de ruta | Validación de directorio temporal y extensión `.msi` antes de ejecutar |
| S1192 | `ZKTeco/ZKTecoScannerService.cs` | Literales de cadena duplicados | Extraído a constantes `DeviceDisconnectedMessage` y `NoDeviceConnectedMessage` |

### Cobertura de código

| Causa del 0% | Fix aplicado |
|--------------|--------------|
| Trigger `pull_request: branches: [master]` en CI | Eliminada la restricción de rama; ahora cualquier PR que toque `middleware/**` ejecuta CI |
| Formato Cobertura no garantizado | Creado `Trazzo.Biometric.Agent.Tests/coverlet.runsettings` con `<Format>cobertura</Format>` explícito |
| `sonarscanner begin` sin ruta de reporte | Añadido `/d:sonar.cs.cobertura.reportsPaths="TestResults\*\coverage.cobertura.xml"` |
| `AutoUpdateService` sin tests | Creado `AutoUpdateServiceTests.cs` con 8 tests |
| `AgentHealthService` sin tests | Creado `AgentHealthServiceTests.cs` con 4 tests |
| `ZKTecoNativeSdk` sin cobertura posible | `[ExcludeFromCodeCoverage]` legítimo (P/Invoke wrapper, requiere hardware ZK9500) |

---

## Cómo ejecutar lint, tests y cobertura localmente

### Prerrequisitos

```powershell
# .NET 10 SDK
dotnet --version  # debe mostrar 10.x.x

# La DLL del SDK ZKTeco debe estar en su ubicación esperada
# (o el proyecto compila igual porque la referencia es Copy=PreserveNewest)
```

### Compilar

```powershell
cd middleware
dotnet restore
dotnet build -c Release --no-restore
```

### Ejecutar tests con cobertura

```powershell
cd middleware
dotnet test Trazzo.Biometric.Agent.Tests/Trazzo.Biometric.Agent.Tests.csproj `
  -c Release `
  --no-restore `
  --logger "trx;LogFileName=test-results.trx" `
  --results-directory TestResults `
  --collect:"XPlat Code Coverage" `
  --settings Trazzo.Biometric.Agent.Tests/coverlet.runsettings
```

El reporte de cobertura se genera en:
```
middleware/TestResults/<guid>/coverage.cobertura.xml
```

### Ver cobertura en HTML (opcional)

```powershell
# Instalar reportgenerator si no está instalado
dotnet tool install --global dotnet-reportgenerator-globaltool

# Generar reporte HTML
reportgenerator `
  -reports:"TestResults\*\coverage.cobertura.xml" `
  -targetdir:"TestResults\coverage-report" `
  -reporttypes:Html
```

Abrir `TestResults/coverage-report/index.html` en el navegador.

---

## Cómo SonarCloud recibe la cobertura

El flujo en CI (`.github/workflows/middleware-ci.yml`) es:

1. `dotnet sonarscanner begin` — inicia el análisis con la ruta de reportes configurada.
2. `dotnet build` — compila el proyecto instrumentado.
3. `dotnet test ... --collect:"XPlat Code Coverage" --settings coverlet.runsettings` — genera `coverage.cobertura.xml`.
4. `dotnet sonarscanner end` — envía el análisis a SonarCloud, incluyendo el XML de cobertura.
5. `SonarSource/sonarqube-quality-gate-action` — verifica el Quality Gate y falla el workflow si no se supera.

**Parámetros clave de SonarCloud:**
```
/d:sonar.cs.cobertura.reportsPaths="TestResults\*\coverage.cobertura.xml"
/d:sonar.coverage.exclusions="**/ZKTeco/ZKTecoNativeSdk.cs,**/Program.cs,**/Properties/**"
/d:sonar.exclusions="**/Native/**,**/bin/**,**/obj/**,**/tmp-agent-test/**"
```

---

## Security Hotspots — acciones manuales pendientes en SonarCloud

Una vez que el análisis de SonarCloud se ejecute en el PR o en `master`, se deben marcar los dos hotspots como revisados:

| Hotspot | Regla | Archivo | Acción en SonarCloud |
|---------|-------|---------|----------------------|
| `Process.Start` con ruta de MSI | S4721 | `AutoUpdate/AutoUpdateService.cs` | **Mark as Safe** — ruta validada (temp dir + `.msi`) y hash SHA-256 verificado. Ver `docs/sonar-security-review.md`. |
| Comparación de esquema HTTP/HTTPS | S5332 | `AutoUpdateService.cs`, `HybridCryptographyService.cs` | **Mark as Safe** — la comparación es la guardia que bloquea HTTP. Ver `docs/sonar-security-review.md`. |

Los argumentos técnicos detallados están en [`docs/sonar-security-review.md`](./sonar-security-review.md).

---

## Exclusiones de cobertura justificadas

| Archivo excluido | Razón |
|-----------------|-------|
| `ZKTeco/ZKTecoNativeSdk.cs` | Thin P/Invoke wrapper sobre `libzkfpcsharp.dll`. Requiere hardware físico ZK9500. Marcado con `[ExcludeFromCodeCoverage]`. |
| `Program.cs` | Entry point de host genérico. Sin lógica de negocio testeable. |
| `Properties/**` | Metadata de ensamblado generada. |
| `Native/**` | DLL binaria del SDK ZKTeco (no es código C#). |

No se excluye ningún servicio ni lógica de negocio para inflar artificialmente el porcentaje de cobertura.

---

## Estado del Quality Gate

Objetivo para aprobar:

| Métrica | Umbral | Estado esperado |
|---------|--------|-----------------|
| Security Rating | A | ✅ Corregido (HTTPS enforced + path validation) |
| Coverage (new code) | ≥ 80% | ✅ Corregido (nuevos tests + runsettings) |
| Security Hotspots Reviewed | 100% | ⏳ Pendiente revisión manual en SonarCloud |
| New Issues | 0 | ✅ Corregido (S6966, S5332, S4721, S1192) |
