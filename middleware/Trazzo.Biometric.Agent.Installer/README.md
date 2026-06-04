# Trazzo Biometric Agent — Instalador MSI

Proyecto WiX Toolset v4 que genera el instalador MSI para el **Trazzo Biometric Agent**.

El MSI instala el agente como Windows Service, incluye el runtime de .NET 10 (self-contained) y configura la recuperación automática ante fallos.

---

## Estructura

```text
Trazzo.Biometric.Agent.Installer/
├── Product.wxs                      # Definición principal del paquete MSI
├── Trazzo.Biometric.Agent.Installer.wixproj
├── Resources/
│   ├── banner.bmp                   # Franja superior del instalador (493×58 px, 24bpp)
│   └── dialog.bmp                   # Panel izquierdo de bienvenida/fin (493×312 px, 24bpp)
└── scripts/
    └── GenerateInstallerWxs.ps1     # Genera el fragmento WXS con todos los archivos publicados
```

---

## Requisitos

- [WiX Toolset SDK v4](https://wixtoolset.org/) (se restaura automáticamente como NuGet)
- .NET SDK 10 (para compilar y publicar el agente antes de empaquetar)
- La DLL del SDK ZKTeco en la ruta correcta:

```text
..\Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll
```

Si falta la DLL, el build falla con un error explícito antes de generar el MSI.

---

## Generar el MSI

```powershell
dotnet build .\Trazzo.Biometric.Agent.Installer.wixproj -c Release
```

El MSI queda en:

```text
bin\Release\Trazzo.Biometric.Agent.msi
```

### Qué hace el build

1. **Publica el agente** con `dotnet publish --self-contained true -r win-x64`. Incluye el runtime de .NET 10 — la PC de destino no necesita tener .NET instalado.
2. **Verifica la DLL nativa** (`libzkfpcsharp.dll`). Falla el build si no existe.
3. **Ejecuta `GenerateInstallerWxs.ps1`** para crear `obj\Release\Product.Generated.wxs` con la lista completa de archivos del publish. Los IDs de componente y GUIDs son deterministas (basados en MD5 del path relativo), lo que garantiza actualizaciones in-place correctas.
4. **Compila con WiX v4** enlazando `Product.wxs` + el WXS generado.

---

## Flujo del Instalador (UI)

El instalador usa `WixUI_InstallDir` que presenta las siguientes pantallas:

| Pantalla | Descripción |
|---|---|
| Bienvenida | Logo en panel izquierdo (`dialog.bmp`), texto de introducción |
| Selección de carpeta | Default: `C:\Program Files\Trazzo\BiometricAgent\` |
| Confirmación | Resumen antes de instalar |
| Progreso | Barra de progreso durante la copia de archivos |
| Finalización | Confirmación de instalación exitosa |

---

## Imágenes del Instalador

| Archivo | Dimensiones | Aparece en |
|---|---|---|
| `Resources\banner.bmp` | 493×58 px, 24bpp | Franja superior de todas las pantallas |
| `Resources\dialog.bmp` | 493×312 px, 24bpp | Panel izquierdo en bienvenida y finalización |

Las imágenes deben ser BMP de 24 bits por píxel (sin canal alfa). Si se reemplazan, mantener exactamente esas dimensiones para que WiX las encaje correctamente.

---

## Servicio Windows instalado

El MSI registra el servicio con los siguientes parámetros:

| Parámetro | Valor |
|---|---|
| Nombre interno | `TrazzoAgent` |
| Display name | `Trazzo Biometric Agent` |
| Tipo de inicio | Automático |
| Cuenta | `LocalSystem` |
| Puerto WebSocket | `localhost:9001` |

### Recuperación ante fallos

Configurada vía `util:ServiceConfig` de `WixToolset.Util.wixext`:

| Fallo | Acción | Demora |
|---|---|---|
| 1er crash | Reinicio automático | 10 segundos |
| 2do crash | Reinicio automático | 10 segundos |
| 3er crash | Reinicio automático | 10 segundos |
| Reset del contador | Tras 24 h de funcionamiento estable | — |

---

## Instalación

### Interactiva

Doble clic en el MSI. Si ya hay una versión instalada (igual o anterior) se desinstala automáticamente antes de instalar la nueva — no es necesario desinstalar a mano ni abrir ningún comando.

### Silenciosa (GPO, SCCM, despliegue masivo)

```powershell
msiexec /i Trazzo.Biometric.Agent.msi /quiet /norestart
```

### Con log de instalación

```powershell
msiexec /i Trazzo.Biometric.Agent.msi /l*v install.log
```

### Desinstalar

```powershell
msiexec /x Trazzo.Biometric.Agent.msi
```

## Reinstalación limpia

El instalador está configurado con `AllowSameVersionUpgrades="yes"`, lo que permite reinstalar el mismo MSI sin error. En cada desinstalación o reinstalación, el directorio `%PROGRAMDATA%\TrazzoAgent\` se elimina automáticamente (base de datos SQLite, caché de clave RSA), dejando el agente en estado de primera instalación.

El flujo es siempre el mismo sin importar si es primera vez o una reinstalación:

```
Doble clic en MSI
  → Desinstala versión anterior (si existe) + limpia %PROGRAMDATA%\TrazzoAgent\
  → Instala archivos frescos en Program Files
  → Registra y arranca el servicio TrazzoAgent
```

---

## GenerateInstallerWxs.ps1

Script PowerShell que se ejecuta automáticamente en el build. Toma todos los archivos del directorio `obj\publish` y genera un fragmento WXS con:

- Estructura de directorios espejando la del publish
- Un `Component` por archivo, con GUID determinista
- `ServiceInstall` + `ServiceControl` + `util:ServiceConfig` en el componente del ejecutable principal (`Trazzo.Biometric.Agent.exe`)
- Excluye `appsettings.Development.json` y archivos `.pdb`

Los GUIDs son deterministas: dado el mismo conjunto de archivos, el script siempre genera los mismos GUIDs, lo que permite actualizaciones in-place sin romper el registro de Windows Installer.

---

## Pendiente

| Qué | Detalle |
|---|---|
| Firma digital | Firmar el MSI con un certificado de código para evitar alertas de SmartScreen |
| Versión desde ensamblado | Leer la versión del `.csproj` del agente en vez del valor fijo `1.0.0` en `.wixproj` |
