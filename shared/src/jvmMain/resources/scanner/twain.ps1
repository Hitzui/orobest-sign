param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('List', 'Scan')]
    [string] $Action,
    [string] $DeviceId = ''
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
# Driver messages on stdout/stderr are not part of the bridge protocol.
# Publish a complete UTF-8 response independently, after the source is closed.
$response = $null
try {
    $library = Join-Path $PSScriptRoot 'NTwain.dll'
    Add-Type -Path $library
    Add-Type -Path (Join-Path $PSScriptRoot 'TwainBridge.cs') -ReferencedAssemblies @($library, 'System.Drawing.dll', 'System.Core.dll')
    $response = [Orobest.Scanner.TwainBridge]::Run($Action, $DeviceId, (Join-Path $PSScriptRoot 'scan.bmp'))
} catch {
    $response = @{ status = 'error'; code = 'twain_unavailable' }
} finally {
    $json = $response | ConvertTo-Json -Compress -Depth 5
    $temporaryResponse = Join-Path $PSScriptRoot 'result.json.tmp'
    [System.IO.File]::WriteAllText($temporaryResponse, $json, (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::Move($temporaryResponse, (Join-Path $PSScriptRoot 'result.json'))
}
