<#
Uso recomendado para generar el MSI corporativo:

  .\scripts\Install-Agent.ps1 -BuildMsi

Ese comando compila el proyecto WiX `Trazzo.Biometric.Agent.Installer`, que a su vez ejecuta
`dotnet publish -c Release` del agente y empaqueta el resultado en un MSI. Si necesitas incluir
el SDK de ZKTeco, coloca primero la DLL oficial x64 en:

  Trazzo.Biometric.Agent\Native\x64\libzkfpcsharp.dll

El MSI queda en:

  ..\Trazzo.Biometric.Agent.Installer\bin\Release\Trazzo.Biometric.Agent.msi
#>

param(
    [string]$PublishDir = "$PSScriptRoot\..\bin\Release\net10.0-windows\win-x64\publish",
    [string]$InstallerProject = "$PSScriptRoot\..\..\Trazzo.Biometric.Agent.Installer\Trazzo.Biometric.Agent.Installer.wixproj",
    [string]$InstallDir = "C:\Program Files\Trazzo\BiometricAgent",
    [string]$ServiceName = "TrazzoAgent",
    [string]$ServiceDisplayName = "Trazzo Biometric Agent",
    [switch]$BuildMsi,
    [switch]$Uninstall
)

$ErrorActionPreference = "Stop"

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if ($BuildMsi) {
    Write-Host "=== Generando MSI de Trazzo Biometric Agent ===" -ForegroundColor Cyan

    $sdkDll = Join-Path $PSScriptRoot "..\Native\x64\libzkfpcsharp.dll"
    if (-not (Test-Path $sdkDll)) {
        Write-Warning "libzkfpcsharp.dll no fue encontrada en Native\x64\. El MSI se generara sin la DLL del SDK ZKTeco."
        Write-Warning "Para incluirla, coloque la DLL oficial x64 en: $sdkDll"
    }

    dotnet build $InstallerProject -c Release

    $installerDir = Split-Path -Parent $InstallerProject
    $msiPath = Join-Path $installerDir "bin\Release\es-ES\Trazzo.Biometric.Agent.msi"
    Write-Host ""
    Write-Host "MSI generado: $msiPath" -ForegroundColor Green
    exit 0
}

if (-not (Test-IsAdministrator)) {
    throw "Ejecute este script como administrador para instalar o desinstalar el servicio. Para solo generar el MSI use -BuildMsi."
}

function Stop-AgentService {
    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -eq "Running") {
        Write-Host "Deteniendo el servicio $ServiceName..."
        Stop-Service -Name $ServiceName -Force
        $svc.WaitForStatus("Stopped", (New-TimeSpan -Seconds 15))
    }
}

function Remove-AgentService {
    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($svc) {
        Write-Host "Eliminando el servicio $ServiceName..."
        & sc.exe delete $ServiceName | Out-Null
        Start-Sleep -Seconds 1
    }
}

if ($Uninstall) {
    Write-Host "=== Desinstalando Trazzo Biometric Agent ===" -ForegroundColor Yellow
    Stop-AgentService
    Remove-AgentService
    if (Test-Path $InstallDir) {
        Remove-Item -Recurse -Force $InstallDir
        Write-Host "Directorio eliminado: $InstallDir"
    }
    Write-Host "Desinstalacion completada." -ForegroundColor Green
    exit 0
}

Write-Host "=== Instalando Trazzo Biometric Agent ===" -ForegroundColor Cyan

if (-not (Test-Path $PublishDir)) {
    Write-Error "Directorio de publicacion no encontrado: $PublishDir`nEjecute primero: dotnet publish -c Release"
}

$exePath = Join-Path $PublishDir "Trazzo.Biometric.Agent.exe"
if (-not (Test-Path $exePath)) {
    Write-Error "Ejecutable no encontrado: $exePath"
}

Stop-AgentService
Remove-AgentService

Write-Host "Copiando binarios a $InstallDir..."
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null

$nativeDir = Join-Path $InstallDir "Native\x64"
New-Item -ItemType Directory -Force -Path $nativeDir | Out-Null

Copy-Item -Path "$PublishDir\*" -Destination $InstallDir -Recurse -Force

$sdkDll = Join-Path $PSScriptRoot "..\Native\x64\libzkfpcsharp.dll"
if (Test-Path $sdkDll) {
    Write-Host "Copiando SDK ZKTeco: libzkfpcsharp.dll..."
    Copy-Item -Path $sdkDll -Destination $nativeDir -Force
} else {
    Write-Warning "libzkfpcsharp.dll no encontrada en Native\x64\. Coloquela manualmente en: $nativeDir"
}

Write-Host "Registrando el servicio de Windows..."
$fullExePath = Join-Path $InstallDir "Trazzo.Biometric.Agent.exe"
& sc.exe create $ServiceName `
    binPath= "`"$fullExePath`"" `
    DisplayName= $ServiceDisplayName `
    start= auto | Out-Null

& sc.exe description $ServiceName "Agente biometrico de Trazzo. Conecta el lector ZKTeco ZK9500 con el backend via WebSocket local." | Out-Null

Write-Host "Iniciando el servicio..."
Start-Service -Name $ServiceName

$svc = Get-Service -Name $ServiceName
Write-Host "Estado del servicio: $($svc.Status)" -ForegroundColor $(if ($svc.Status -eq "Running") { "Green" } else { "Red" })

Write-Host ""
Write-Host "=== Instalacion completada ===" -ForegroundColor Green
Write-Host "  Directorio : $InstallDir"
Write-Host "  Servicio   : $ServiceName ($ServiceDisplayName)"
Write-Host "  WebSocket  : ws://localhost:9001"
Write-Host ""
Write-Host "Para configurar el tenant sin editar JSON a mano ejecute:"
Write-Host "  `$token = Read-Host 'Queue:AgentToken' -AsSecureString"
Write-Host "  $InstallDir\Configure-Agent.ps1 -TenantId <uuid> -DeviceCode <device.code> -BackendBaseUrl <url> -AgentTokenSecure `$token"
