# Trazzo Middleware — Índice

Documentación completa en cada subcarpeta:

| README | Contenido |
|--------|-----------|
| [Trazzo.Biometric.Agent](./Trazzo.Biometric.Agent/README.md) | Arquitectura, configuración, protocolo WebSocket, seguridad, errores comunes |
| [Trazzo.Biometric.Agent.Tests](./Trazzo.Biometric.Agent.Tests/README.md) | Tests xUnit, cobertura, fakes |
| [Native/x64](./Trazzo.Biometric.Agent/Native/x64/README.md) | Cómo colocar la DLL del SDK ZKTeco |

---

## Comandos principales

### Generar el MSI (desarrollador)

Compila el agente y empaqueta todo en un instalador `.msi` listo para entregar al cliente. Incluye el runtime de .NET 10 — la PC del colegio no necesita tener .NET instalado.

```powershell
cd middleware\Trazzo.Biometric.Agent.Installer
dotnet build -c Release
```

El MSI queda en:
```
bin\Release\es-ES\Trazzo.Biometric.Agent.msi
```

---

### Instalar (usuario final — doble clic)

El usuario recibe el `.msi` y hace doble clic. El instalador es visual: Siguiente → Instalar → Finalizar. El servicio arranca automáticamente al terminar, sin reiniciar.

Para instalación silenciosa (despliegue masivo con GPO o SCCM):

```powershell
msiexec /i Trazzo.Biometric.Agent.msi /quiet /norestart
```

---

### Desinstalar

**Opción A — Panel de control:**
Panel de control → Programas → Desinstalar un programa → `Trazzo Biometric Agent` → Desinstalar

**Opción B — Doble clic en el mismo MSI:**
Doble clic en `Trazzo.Biometric.Agent.msi` → Quitar

Elimina el servicio, los archivos de `C:\Program Files\Trazzo\BiometricAgent\` y los datos de `C:\ProgramData\TrazzoAgent\`.

Para desinstalación silenciosa:

```powershell
msiexec /x Trazzo.Biometric.Agent.msi /quiet /norestart
```

---

### Verificar que el servicio está corriendo

Abrir el navegador y entrar a:
```
http://localhost:9001/
```

Debe responder:
```json
{"type":"health.check.result","success":true}
```
