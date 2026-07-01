param(
    [string]$Username = "LeninTF",
    [string]$Repo = "Trazzo",
    [int]$KeepLatest = 10,
    [switch]$WhatIf = $true,
    [string[]]$Packages = @("trazzo-back", "trazzo-front")
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command "gh" -ErrorAction SilentlyContinue)) {
    Write-Error "gh CLI no encontrado. Instala desde https://cli.github.com/ y ejecuta 'gh auth login'"
    exit 1
}

$repoLower = $Repo.ToLowerInvariant()

foreach ($pkg in $Packages) {
    Write-Host "`n========== Procesando: $pkg ==========" -ForegroundColor Cyan

    $encodedPkg = "$repoLower/$pkg" -replace '/', '%2F'
    $uri = "/users/$Username/packages/container/$encodedPkg/versions"

    try {
        $versions = gh api $uri --paginate | ConvertFrom-Json
    }
    catch {
        Write-Error "Error al consultar API. Autentícate con 'gh auth login' (scopes: read:packages, delete:packages, write:packages).`nDetalle: $_"
        exit 1
    }

    if (-not $versions -or $versions.Count -eq 0) {
        Write-Host "  No se encontraron versiones." -ForegroundColor Yellow
        continue
    }

    Write-Host "  Total versiones: $($versions.Count)" -ForegroundColor Cyan

    $sorted = $versions | Sort-Object -Property created_at -Descending
    $toKeep = $sorted | Select-Object -First $KeepLatest
    $toDelete = $sorted | Select-Object -Skip $KeepLatest

    Write-Host "  A mantener: $($toKeep.Count) versión(es)" -ForegroundColor Green
    foreach ($v in $toKeep) {
        $tags = ($v.metadata.container.tags -join ', ')
        if (-not $tags) { $tags = "〈sin tag〉" }
        Write-Host "    [$($v.id)] $($v.created_at) | tags: $tags" -ForegroundColor Green
    }

    if ($toDelete.Count -eq 0) {
        Write-Host "  No hay versiones para eliminar." -ForegroundColor Yellow
        continue
    }

    Write-Host "  A eliminar: $($toDelete.Count) versión(es)" -ForegroundColor Red
    foreach ($v in $toDelete) {
        $tags = ($v.metadata.container.tags -join ', ')
        if (-not $tags) { $tags = "〈sin tag〉" }
        Write-Host "    [$($v.id)] $($v.created_at) | tags: $tags" -ForegroundColor Red
    }

    if ($WhatIf) {
        Write-Host "  [DRY-RUN] No se eliminó nada. Ejecuta con -WhatIf:`$false para borrar." -ForegroundColor Magenta
        continue
    }

    $confirm = Read-Host "  Eliminar estas $($toDelete.Count) versiones? (si/no)"
    if ($confirm -ne "si") {
        Write-Host "  Omitido." -ForegroundColor Yellow
        continue
    }

    foreach ($v in $toDelete) {
        $deleteUri = "/users/$Username/packages/container/$encodedPkg/versions/$($v.id)"
        try {
            gh api --method DELETE $deleteUri --silent
            Write-Host "  Eliminado [$($v.id)] $($v.created_at)" -ForegroundColor Red
        }
        catch {
            Write-Warning "  Fallo al eliminar [$($v.id)]: $_"
        }
    }
}

Write-Host "`n=== Proceso completado ===" -ForegroundColor Cyan