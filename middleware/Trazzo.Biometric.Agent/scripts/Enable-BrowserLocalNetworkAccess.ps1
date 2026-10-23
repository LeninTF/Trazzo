<#
.SYNOPSIS
    Autoriza a la web de Trazzo a conectarse al agente biométrico local (localhost:9001).

.DESCRIPTION
    Chrome 142+ (y Edge equivalente) bloquean que una página pública abra conexiones a la red
    local o a localhost: es la restricción "Local Network Access" (LNA). Desde Chrome 147 aplica
    también a WebSockets, que es como el frontend habla con este agente. El síntoma en la consola
    del navegador es:

        WebSocket connection to 'ws://localhost:9001/' failed:
        net::ERR_BLOCKED_BY_LOCAL_NETWORK_ACCESS_CHECKS

    El bloqueo NO se resuelve desde el agente: el navegador lo decide antes de enviar la petición
    y exige permiso explícito. Este script aplica la política de empresa
    `LocalNetworkAccessAllowedForUrls`, que concede ese permiso para los orígenes indicados en
    todas las sesiones del equipo, sin depender de que cada usuario acepte un aviso.

    Requiere ejecutarse como Administrador (escribe en HKLM).

.PARAMETER Origins
    Orígenes a autorizar (esquema + host, sin ruta). Ej: https://trazzosaas.noahtechperu.com

.PARAMETER Remove
    Revierte la política, eliminando las claves creadas.

.EXAMPLE
    .\Enable-BrowserLocalNetworkAccess.ps1 -Origins "https://trazzosaas.noahtechperu.com"

.EXAMPLE
    .\Enable-BrowserLocalNetworkAccess.ps1 -Origins "https://trazzosaas.noahtechperu.com" -Remove
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string[]] $Origins,

    [switch] $Remove
)

$ErrorActionPreference = 'Stop'

# Chrome y Edge comparten el esquema de políticas (ambos son Chromium).
$policyRoots = @(
    @{ Name = 'Google Chrome'; Path = 'HKLM:\SOFTWARE\Policies\Google\Chrome' },
    @{ Name = 'Microsoft Edge'; Path = 'HKLM:\SOFTWARE\Policies\Microsoft\Edge' }
)

# IMPORTANTE: el agente escucha en localhost, que para Chromium es el espacio "loopback",
# NO la "red local" (IPs privadas tipo 192.168.x.x). Son políticas distintas y solo la de
# loopback destraba ws://localhost. Se escriben además las variantes de nombre porque
# difieren entre versiones de Chrome/Edge; las que el navegador no conozca simplemente se
# ignoran (aparecen como desconocidas en chrome://policy).
$policyNames = @(
    'LoopbackNetworkAccessAllowedForUrls',  # localhost / 127.0.0.1  <- la que aplica aquí
    'LoopbackNetworkAllowedForUrls',        # variante de nombre (Edge / versiones nuevas)
    'LocalNetworkAccessAllowedForUrls',     # red local (IPs privadas)
    'LocalNetworkAllowedForUrls'            # variante de nombre
)

function Assert-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "Este script debe ejecutarse como Administrador (escribe políticas en HKLM)."
    }
}

function Set-LocalNetworkAccessPolicy {
    param([string] $BrowserName, [string] $BrowserPolicyPath, [string[]] $AllowedOrigins)

    foreach ($policyName in $policyNames) {
        $listPath = Join-Path $BrowserPolicyPath $policyName

        if (-not (Test-Path $listPath)) {
            New-Item -Path $listPath -Force | Out-Null
        }

        # La política es una lista: cada entrada es un valor con nombre "1", "2", ... Se reescribe
        # completa para que el resultado sea idempotente al re-ejecutar el script.
        Get-Item -Path $listPath | Select-Object -ExpandProperty Property | ForEach-Object {
            Remove-ItemProperty -Path $listPath -Name $_ -ErrorAction SilentlyContinue
        }

        $index = 1
        foreach ($origin in $AllowedOrigins) {
            New-ItemProperty -Path $listPath -Name "$index" -Value $origin -PropertyType String -Force | Out-Null
            $index++
        }
    }

    Write-Host "[$BrowserName] Autorizado ($($policyNames.Count) políticas): $($AllowedOrigins -join ', ')" -ForegroundColor Green
}

function Remove-LocalNetworkAccessPolicy {
    param([string] $BrowserName, [string] $BrowserPolicyPath)

    $removed = 0
    foreach ($policyName in $policyNames) {
        $listPath = Join-Path $BrowserPolicyPath $policyName
        if (Test-Path $listPath) {
            Remove-Item -Path $listPath -Recurse -Force
            $removed++
        }
    }

    if ($removed -gt 0) { Write-Host "[$BrowserName] $removed política(s) eliminada(s)." -ForegroundColor Yellow }
    else { Write-Host "[$BrowserName] No había política que eliminar." -ForegroundColor DarkGray }
}

Assert-Administrator

# Validación: la política espera origen (esquema + host), no una URL con ruta.
foreach ($origin in $Origins) {
    if ($origin -notmatch '^https?://[^/]+/?$') {
        throw "Origen inválido: '$origin'. Use solo esquema + host, por ejemplo https://trazzosaas.noahtechperu.com"
    }
}
$normalizedOrigins = $Origins | ForEach-Object { $_.TrimEnd('/') }

foreach ($root in $policyRoots) {
    if ($Remove) {
        Remove-LocalNetworkAccessPolicy -BrowserName $root.Name -BrowserPolicyPath $root.Path
    }
    else {
        Set-LocalNetworkAccessPolicy -BrowserName $root.Name -BrowserPolicyPath $root.Path -AllowedOrigins $normalizedOrigins
    }
}

Write-Host ""
Write-Host "Listo. Cierre COMPLETAMENTE el navegador (revise que no queden procesos en segundo" -ForegroundColor Cyan
Write-Host "plano) y vuelva a abrirlo para que tome la política." -ForegroundColor Cyan
Write-Host "Verificación: abra chrome://policy -> 'Volver a cargar políticas' y confirme que" -ForegroundColor Cyan
Write-Host "'LoopbackNetworkAccessAllowedForUrls' aparece con el origen configurado." -ForegroundColor Cyan
