# Trazzo Middleware — Índice

Documentación completa en cada subcarpeta:

| README | Contenido |
|--------|-----------|
| [Trazzo.Biometric.Agent](./Trazzo.Biometric.Agent/README.md) | Arquitectura, configuración, protocolo WebSocket, seguridad, errores comunes |
| [Trazzo.Biometric.Agent.Tests](./Trazzo.Biometric.Agent.Tests/README.md) | Tests xUnit (`dotnet test Trazzo.Middleware.slnx`), cobertura, fakes |
| [Trazzo.Biometric.Agent.Installer](./Trazzo.Biometric.Agent.Installer/README.md) | Generación del MSI con WiX v4, flujo del instalador, imágenes |
| [Native/x64](./Trazzo.Biometric.Agent/Native/x64/README.md) | Cómo colocar la DLL del SDK ZKTeco |
| [Dev handoff](./docs/README-dev-handoff.md) | Pendientes de integración: WebSocket, backend real, hardware, despliegue |

---

## Comandos principales

## Estado para quien retome

El middleware ya queda listo a nivel de código y MSI para los contratos de asistencia/biometría. Lo que falta no es implementación base del agente, sino validación con backend real, lector ZKTeco real y decidir si el WebSocket local debe devolver el `AttendanceProfile` completo al frontend. Ver [Dev handoff](./docs/README-dev-handoff.md) antes de cambiar código.

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

Elimina el servicio, los archivos de `C:\Program Files\Trazzo\BiometricAgent\` y los datos de `C:\Windows\System32\config\systemprofile\AppData\Local\TrazzoAgent\` (y los de versiones anteriores en `C:\ProgramData\TrazzoAgent\`).

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

### Configurar tenant sin editar JSON

Después de instalar el MSI, en el panel del tenant: **Dispositivos** → **Emitir token**. El panel muestra una sola vez el token y el comando de configuración, con el tenant, el lector, el backend y la huella de su clave pública. Se ejecuta en PowerShell como administrador, en `C:\Program Files\Trazzo\BiometricAgent\`:

```powershell
.\Configure-Agent.ps1 -TenantId '00000000-0000-0000-0000-000000000000' -DeviceCode 'ZK-C2PRO-00123' -BackendBaseUrl 'https://trazzosaas.noahtechperu.com/api/v1' -PinnedPublicKeySha256 '<huella-sha256-del-panel>' -AgentTokenSecure (Read-Host "AgentToken" -AsSecureString)
```

`-AllowedOrigins` vale por defecto `https://trazzosaas.noahtechperu.com`; si el front se sirve en otro dominio, se añade `-AllowedOrigins "https://<dominio-del-front>"`.

El script guarda el token del dispositivo (`trzd_…`, pantalla «Dispositivos» del panel) cifrado en `Queue:AgentTokenProtected` usando DPAPI `LocalMachine`; no deja el token en claro en `Queue:AgentToken`.
