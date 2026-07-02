<#
Configura una instalacion del Trazzo Biometric Agent sin editar appsettings.json a mano.

Ejemplo interactivo:
  .\Configure-Agent.ps1 `
    -TenantId "00000000-0000-0000-0000-000000000000" `
    -DeviceCode "ZK-C2PRO-00123" `
    -BackendBaseUrl "https://api.trazzo.pe/api/v1" `
    -AllowedOrigins "https://app.trazzo.pe"

Ejemplo con token seguro:
  .\Configure-Agent.ps1 `
    -TenantId "00000000-0000-0000-0000-000000000000" `
    -DeviceCode "ZK-C2PRO-00123" `
    -BackendBaseUrl "https://api.trazzo.pe/api/v1" `
    -AgentTokenSecure (Read-Host "Queue:AgentToken" -AsSecureString) `
    -AllowedOrigins "https://app.trazzo.pe"
#>

param(
    [string]$ConfigPath = "C:\Program Files\Trazzo\BiometricAgent\appsettings.json",

    [Parameter(Mandatory = $true)]
    [Guid]$TenantId,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$DeviceCode,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$BackendBaseUrl,

    [string]$AgentToken,

    [SecureString]$AgentTokenSecure,

    [string[]]$AllowedOrigins = @("https://app.trazzo.pe"),

    [string]$ServiceName = "TrazzoAgent",

    [switch]$SkipAclHardening,

    [switch]$NoRestart
)

$ErrorActionPreference = "Stop"

if (-not ("TrazzoAgentProvisioning.Dpapi" -as [type])) {
    Add-Type -TypeDefinition @"
using System;
using System.ComponentModel;
using System.Runtime.InteropServices;
using System.Text;

namespace TrazzoAgentProvisioning
{
    public static class Dpapi
    {
        private const int CryptProtectLocalMachine = 0x4;
        private const int CryptProtectUiForbidden = 0x1;
        private const string Prefix = "dpapi-localmachine:";
        private static readonly byte[] Entropy =
            Encoding.UTF8.GetBytes("Trazzo.Biometric.Agent.Queue.AgentToken.v1");

        public static string Protect(string token)
        {
            if (String.IsNullOrWhiteSpace(token))
                throw new ArgumentException("El token no puede estar vacio.", "token");

            byte[] plaintext = Encoding.UTF8.GetBytes(token);
            try
            {
                byte[] protectedBytes = ProtectBytes(plaintext, Entropy);
                return Prefix + Convert.ToBase64String(protectedBytes);
            }
            finally
            {
                Array.Clear(plaintext, 0, plaintext.Length);
            }
        }

        private static byte[] ProtectBytes(byte[] plaintext, byte[] entropy)
        {
            DataBlob dataIn = CreateBlob(plaintext);
            DataBlob entropyBlob = CreateBlob(entropy);
            try
            {
                DataBlob dataOut;
                bool success = CryptProtectData(
                    ref dataIn,
                    "Trazzo Biometric Agent token",
                    ref entropyBlob,
                    IntPtr.Zero,
                    IntPtr.Zero,
                    CryptProtectLocalMachine | CryptProtectUiForbidden,
                    out dataOut);
                if (!success)
                    throw new Win32Exception(Marshal.GetLastWin32Error());

                return BlobToArrayAndFree(dataOut);
            }
            finally
            {
                FreeBlob(dataIn);
                FreeBlob(entropyBlob);
            }
        }

        private static DataBlob CreateBlob(byte[] data)
        {
            IntPtr buffer = Marshal.AllocHGlobal(data.Length);
            Marshal.Copy(data, 0, buffer, data.Length);
            return new DataBlob { Length = data.Length, Data = buffer };
        }

        private static byte[] BlobToArrayAndFree(DataBlob blob)
        {
            try
            {
                byte[] output = new byte[blob.Length];
                Marshal.Copy(blob.Data, output, 0, blob.Length);
                return output;
            }
            finally
            {
                if (blob.Data != IntPtr.Zero)
                    LocalFree(blob.Data);
            }
        }

        private static void FreeBlob(DataBlob blob)
        {
            if (blob.Data != IntPtr.Zero)
                Marshal.FreeHGlobal(blob.Data);
        }

        [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool CryptProtectData(
            ref DataBlob dataIn,
            string description,
            ref DataBlob optionalEntropy,
            IntPtr reserved,
            IntPtr promptStruct,
            int flags,
            out DataBlob dataOut);

        [DllImport("kernel32.dll")]
        private static extern IntPtr LocalFree(IntPtr handle);

        [StructLayout(LayoutKind.Sequential)]
        private struct DataBlob
        {
            public int Length;
            public IntPtr Data;
        }
    }
}
"@
}

function Get-PlainTextFromSecureString {
    param([SecureString]$SecureValue)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Protect-AgentToken {
    param([Parameter(Mandatory = $true)][string]$Token)

    return [TrazzoAgentProvisioning.Dpapi]::Protect($Token)
}

function Protect-ConfigFileAcl {
    param([Parameter(Mandatory = $true)][string]$Path)

    $acl = Get-Acl -LiteralPath $Path
    $acl.SetAccessRuleProtection($true, $false)

    foreach ($rule in @($acl.Access)) {
        [void]$acl.RemoveAccessRule($rule)
    }

    $system = [Security.Principal.SecurityIdentifier]::new("S-1-5-18")
    $administrators = [Security.Principal.SecurityIdentifier]::new("S-1-5-32-544")

    $systemRule = [Security.AccessControl.FileSystemAccessRule]::new(
        $system,
        [Security.AccessControl.FileSystemRights]::FullControl,
        [Security.AccessControl.AccessControlType]::Allow)
    $adminRule = [Security.AccessControl.FileSystemAccessRule]::new(
        $administrators,
        [Security.AccessControl.FileSystemRights]::FullControl,
        [Security.AccessControl.AccessControlType]::Allow)

    $acl.SetAccessRule($systemRule)
    $acl.SetAccessRule($adminRule)
    Set-Acl -LiteralPath $Path -AclObject $acl
}

function Ensure-ObjectProperty {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Object,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [object]$DefaultValue = ([pscustomobject]@{})
    )

    if (-not $Object.PSObject.Properties[$Name]) {
        $Object | Add-Member -MemberType NoteProperty -Name $Name -Value $DefaultValue
    }
    elseif ($null -eq $Object.$Name) {
        $Object.$Name = $DefaultValue
    }

    return $Object.$Name
}

function Set-EndpointDefault {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Endpoints,

        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    if (-not $Endpoints.PSObject.Properties[$Name]) {
        $Endpoints | Add-Member -MemberType NoteProperty -Name $Name -Value $Value
    }
    elseif ([string]::IsNullOrWhiteSpace([string]$Endpoints.$Name)) {
        $Endpoints.$Name = $Value
    }
}

if (-not (Test-Path -LiteralPath $ConfigPath)) {
    throw "No se encontro appsettings.json en: $ConfigPath"
}

if (-not [Uri]::TryCreate($BackendBaseUrl, [UriKind]::Absolute, [ref]([Uri]$null))) {
    throw "BackendBaseUrl debe ser una URL absoluta. Valor recibido: $BackendBaseUrl"
}

if ([string]::IsNullOrWhiteSpace($AgentToken)) {
    if ($null -eq $AgentTokenSecure) {
        $AgentTokenSecure = Read-Host "Queue:AgentToken" -AsSecureString
    }

    $AgentToken = Get-PlainTextFromSecureString $AgentTokenSecure
}

if ([string]::IsNullOrWhiteSpace($AgentToken)) {
    throw "Queue:AgentToken no puede estar vacio."
}

$resolvedConfigPath = (Resolve-Path -LiteralPath $ConfigPath).Path
$backupPath = "$resolvedConfigPath.bak-$(Get-Date -Format 'yyyyMMddHHmmss')"
Copy-Item -LiteralPath $resolvedConfigPath -Destination $backupPath -Force

$json = Get-Content -LiteralPath $resolvedConfigPath -Raw | ConvertFrom-Json

$agent = Ensure-ObjectProperty -Object $json -Name "Agent"
$backend = Ensure-ObjectProperty -Object $json -Name "Backend"
$queue = Ensure-ObjectProperty -Object $json -Name "Queue"
$endpoints = Ensure-ObjectProperty -Object $backend -Name "Endpoints"

$agent.TenantId = $TenantId.ToString()
$agent.DeviceCode = $DeviceCode
$agent.AllowedOrigins = @($AllowedOrigins)

$backend.BaseUrl = $BackendBaseUrl.TrimEnd("/")
Set-EndpointDefault -Endpoints $endpoints -Name "SecurityPublicKey" -Value "/security/public-key"
Set-EndpointDefault -Endpoints $endpoints -Name "AttendanceMark" -Value "/asistencia/marcar"
Set-EndpointDefault -Endpoints $endpoints -Name "AttendanceSync" -Value "/asistencia/sync"
Set-EndpointDefault -Endpoints $endpoints -Name "BiometricList" -Value "/corehr/biometria"
Set-EndpointDefault -Endpoints $endpoints -Name "StartEnrollment" -Value "/corehr/biometria/enroll/iniciar"
Set-EndpointDefault -Endpoints $endpoints -Name "PendingEnrollment" -Value "/corehr/biometria/enroll/pendiente"
Set-EndpointDefault -Endpoints $endpoints -Name "CompleteEnrollment" -Value "/corehr/biometria/enroll/completar"

if (-not $queue.PSObject.Properties["AgentTokenProtected"]) {
    $queue | Add-Member -MemberType NoteProperty -Name "AgentTokenProtected" -Value ""
}

$queue.AgentTokenProtected = Protect-AgentToken -Token $AgentToken
$queue.AgentToken = ""
$AgentToken = $null

$json |
    ConvertTo-Json -Depth 20 |
    Set-Content -LiteralPath $resolvedConfigPath -Encoding UTF8

if (-not $SkipAclHardening) {
    Protect-ConfigFileAcl -Path $resolvedConfigPath
    Write-Host "ACL endurecida: solo SYSTEM y Administradores pueden leer/escribir appsettings.json."
}

Write-Host "Configuracion actualizada: $resolvedConfigPath" -ForegroundColor Green
Write-Host "Backup creado: $backupPath"
Write-Host "TenantId: $($agent.TenantId)"
Write-Host "DeviceCode: $($agent.DeviceCode)"
Write-Host "Backend:BaseUrl: $($backend.BaseUrl)"
Write-Host "AllowedOrigins: $($agent.AllowedOrigins -join ', ')"
Write-Host "Queue:AgentTokenProtected: configurado con DPAPI LocalMachine"

if ($NoRestart) {
    Write-Host "NoRestart activo. Reinicie $ServiceName manualmente para aplicar cambios."
    exit 0
}

$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($null -eq $service) {
    Write-Warning "Servicio $ServiceName no encontrado. La configuracion se aplicara cuando se instale/inicie el agente."
    exit 0
}

Write-Host "Reiniciando servicio $ServiceName..."
Restart-Service -Name $ServiceName -Force
$service = Get-Service -Name $ServiceName
Write-Host "Estado del servicio: $($service.Status)" -ForegroundColor $(if ($service.Status -eq "Running") { "Green" } else { "Yellow" })
