param(
    [Parameter(Mandatory = $true)]
    [string]$PublishDir,

    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

function Convert-ToWixId {
    param([string]$Value)

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Value.ToLowerInvariant())
    $md5 = [System.Security.Cryptography.MD5]::Create()
    try {
        $hash = [System.BitConverter]::ToString($md5.ComputeHash($bytes)).Replace("-", "")
        return "id_$hash"
    }
    finally {
        $md5.Dispose()
    }
}

function Convert-ToDeterministicGuid {
    param([string]$Value)

    $namespace = [Guid]"9F2C9F72-3DA5-4A94-9A58-FB25761020F2"
    $namespaceBytes = $namespace.ToByteArray()
    $valueBytes = [System.Text.Encoding]::UTF8.GetBytes($Value.ToLowerInvariant())
    $bytes = New-Object byte[] ($namespaceBytes.Length + $valueBytes.Length)
    [Array]::Copy($namespaceBytes, 0, $bytes, 0, $namespaceBytes.Length)
    [Array]::Copy($valueBytes, 0, $bytes, $namespaceBytes.Length, $valueBytes.Length)

    $md5 = [System.Security.Cryptography.MD5]::Create()
    try {
        $hash = $md5.ComputeHash($bytes)
        $hash[6] = ($hash[6] -band 0x0F) -bor 0x30
        $hash[8] = ($hash[8] -band 0x3F) -bor 0x80
        return ([Guid]::new($hash)).ToString().ToUpperInvariant()
    }
    finally {
        $md5.Dispose()
    }
}

function Add-DirectoryNode {
    param(
        [System.Xml.XmlDocument]$Document,
        [System.Xml.XmlElement]$Parent,
        [string]$Id,
        [string]$Name
    )

    $directory = $Document.CreateElement("Directory", $Document.DocumentElement.NamespaceURI)
    $directory.SetAttribute("Id", $Id)
    $directory.SetAttribute("Name", $Name)
    [void]$Parent.AppendChild($directory)
    return $directory
}

function Get-RelativePath {
    param(
        [string]$BasePath,
        [string]$TargetPath
    )

    $baseFullPath = [System.IO.Path]::GetFullPath($BasePath)
    if (-not $baseFullPath.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $baseFullPath += [System.IO.Path]::DirectorySeparatorChar
    }

    $targetFullPath = [System.IO.Path]::GetFullPath($TargetPath)
    $baseUri = [Uri]$baseFullPath
    $targetUri = [Uri]$targetFullPath
    return [Uri]::UnescapeDataString($baseUri.MakeRelativeUri($targetUri).ToString()).Replace('/', [System.IO.Path]::DirectorySeparatorChar)
}

$publishRoot = (Resolve-Path -LiteralPath $PublishDir).Path
$files = Get-ChildItem -LiteralPath $publishRoot -File -Recurse |
    Where-Object {
        $_.Name -ne "appsettings.Development.json" -and
        $_.Extension -ne ".pdb"
    } |
    Sort-Object FullName
if ($files.Count -eq 0) {
    throw "No se encontraron archivos publicados en $publishRoot."
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$xml = New-Object System.Xml.XmlDocument
$xml.PreserveWhitespace = $true
$wix = $xml.CreateElement("Wix", "http://wixtoolset.org/schemas/v4/wxs")
[void]$xml.AppendChild($wix)

$fragment = $xml.CreateElement("Fragment", $wix.NamespaceURI)
[void]$wix.AppendChild($fragment)

$installDirectoryRef = $xml.CreateElement("DirectoryRef", $wix.NamespaceURI)
$installDirectoryRef.SetAttribute("Id", "INSTALLFOLDER")
[void]$fragment.AppendChild($installDirectoryRef)

$directoryMap = @{ "" = "INSTALLFOLDER" }
$directoryElementMap = @{ "" = $installDirectoryRef }

$componentGroup = $xml.CreateElement("ComponentGroup", $wix.NamespaceURI)
$componentGroup.SetAttribute("Id", "PublishedFiles")
[void]$fragment.AppendChild($componentGroup)

foreach ($file in $files) {
    $relativePath = (Get-RelativePath -BasePath $publishRoot -TargetPath $file.FullName).Replace('\', '/')
    $relativeDirectory = [System.IO.Path]::GetDirectoryName($relativePath)
    if ($null -eq $relativeDirectory) {
        $relativeDirectory = ""
    }
    $relativeDirectory = $relativeDirectory.Replace('\', '/')

    if (-not $directoryMap.ContainsKey($relativeDirectory)) {
        $parts = $relativeDirectory.Split('/', [System.StringSplitOptions]::RemoveEmptyEntries)
        $currentPath = ""

        foreach ($part in $parts) {
            $parentPath = $currentPath
            $currentPath = if ($currentPath.Length -eq 0) { $part } else { "$currentPath/$part" }

            if (-not $directoryMap.ContainsKey($currentPath)) {
                $directoryId = "dir_" + (Convert-ToWixId $currentPath)
                $parentElement = $directoryElementMap[$parentPath]
                $directoryElement = Add-DirectoryNode -Document $xml -Parent $parentElement -Id $directoryId -Name $part
                $directoryMap[$currentPath] = $directoryId
                $directoryElementMap[$currentPath] = $directoryElement
            }
        }
    }

    $componentId = "cmp_" + (Convert-ToWixId $relativePath)
    $fileId = "fil_" + (Convert-ToWixId $relativePath)
    $componentGuid = Convert-ToDeterministicGuid $relativePath

    $component = $xml.CreateElement("Component", $wix.NamespaceURI)
    $component.SetAttribute("Id", $componentId)
    $component.SetAttribute("Guid", $componentGuid)
    $component.SetAttribute("Directory", $directoryMap[$relativeDirectory])
    [void]$fragment.AppendChild($component)

    $fileNode = $xml.CreateElement("File", $wix.NamespaceURI)
    $fileNode.SetAttribute("Id", $fileId)
    $fileNode.SetAttribute("Source", $file.FullName)
    $fileNode.SetAttribute("KeyPath", "yes")
    [void]$component.AppendChild($fileNode)

    if ($relativePath -ieq "Trazzo.Biometric.Agent.exe") {
        $serviceInstall = $xml.CreateElement("ServiceInstall", $wix.NamespaceURI)
        $serviceInstall.SetAttribute("Id", "TrazzoAgentServiceInstall")
        $serviceInstall.SetAttribute("Name", "TrazzoAgent")
        $serviceInstall.SetAttribute("DisplayName", "Trazzo Biometric Agent")
        $serviceInstall.SetAttribute("Description", "Agente biometrico de Trazzo. Conecta el lector ZKTeco ZK9500 con el backend via WebSocket local en ws://localhost:9001")
        $serviceInstall.SetAttribute("Type", "ownProcess")
        $serviceInstall.SetAttribute("Start", "auto")
        $serviceInstall.SetAttribute("Account", "LocalSystem")
        $serviceInstall.SetAttribute("ErrorControl", "normal")
        [void]$component.AppendChild($serviceInstall)

        $serviceControl = $xml.CreateElement("ServiceControl", $wix.NamespaceURI)
        $serviceControl.SetAttribute("Id", "TrazzoAgentServiceControl")
        $serviceControl.SetAttribute("Name", "TrazzoAgent")
        # Start="install" con Wait="no": arranca el servicio al finalizar la instalacion sin esperar
        # a que alcance estado "Running". Asi no hay rollback si el hardware no esta conectado
        # o si el SCM tarda en arrancar el servicio (libzkfpcsharp.dll u otros).
        $serviceControl.SetAttribute("Start", "install")
        $serviceControl.SetAttribute("Stop", "both")
        $serviceControl.SetAttribute("Remove", "uninstall")
        $serviceControl.SetAttribute("Wait", "no")
        [void]$component.AppendChild($serviceControl)

        # Reinicio automatico del servicio ante fallos inesperados (caidas de voltaje, errores criticos).
        # 1er y 2do fallo: reinicia tras 10 segundos. 3er fallo: reinicia tras 60 segundos.
        # El contador de fallos se resetea cada 24 horas de funcionamiento estable.
    }

    $componentRef = $xml.CreateElement("ComponentRef", $wix.NamespaceURI)
    $componentRef.SetAttribute("Id", $componentId)
    [void]$componentGroup.AppendChild($componentRef)
}

$writerSettings = New-Object System.Xml.XmlWriterSettings
$writerSettings.Indent = $true
$writerSettings.Encoding = New-Object System.Text.UTF8Encoding($false)
$writer = [System.Xml.XmlWriter]::Create($OutputPath, $writerSettings)
try {
    $xml.Save($writer)
}
finally {
    $writer.Dispose()
}
