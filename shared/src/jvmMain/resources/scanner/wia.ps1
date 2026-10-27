param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('List', 'Scan')]
    [string] $Action,
    [string] $DeviceId = ''
)

# This bridge runs in a dedicated STA Windows PowerShell process. The response
# and scanned bytes stay in the private working directory, apart from driver output.
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$manager = $null
$device = $null
$item = $null
$image = $null
$response = $null

function Set-OptionalProperty($properties, [int] $id, [int] $value) {
    foreach ($property in $properties) {
        if ($property.PropertyID -eq $id) {
            try { $property.Value = $value } catch { }
            break
        }
    }
}

try {
    $manager = New-Object -ComObject WIA.DeviceManager
    if ($Action -eq 'List') {
        $devices = @(
            foreach ($info in $manager.DeviceInfos) {
                if ($info.Type -eq 1) {
                    $name = 'Escaner de Windows'
                    foreach ($property in $info.Properties) {
                        if ($property.PropertyID -eq 7) { $name = [string] $property.Value }
                    }
                    @{ id = [string] $info.DeviceID; name = $name }
                }
            }
        )
        $response = @{ status = 'ok'; devices = $devices }
    } else {
        $match = $null
        foreach ($info in $manager.DeviceInfos) {
            if ($info.Type -eq 1 -and $info.DeviceID -eq $DeviceId) {
                $match = $info
                break
            }
        }
        if ($null -eq $match) {
            $response = @{ status = 'error'; code = 'device_not_found' }
            exit 0
        }
        $device = $match.Connect()
        # Prefer the flatbed for ID cards: never enable an automatic feeder or
        # duplex implicitly. Drivers that omit this property keep their default.
        Set-OptionalProperty $device.Properties 3088 2
        $item = $device.Items.Item(1)
        Set-OptionalProperty $item.Properties 6146 1 # color intent
        Set-OptionalProperty $item.Properties 6147 300 # horizontal resolution
        Set-OptionalProperty $item.Properties 6148 300 # vertical resolution
        Set-OptionalProperty $item.Properties 6149 0 # horizontal start
        Set-OptionalProperty $item.Properties 6150 0 # vertical start
        foreach ($property in $item.Properties) {
            if ($property.PropertyID -in @(6151, 6152)) {
                try {
                    if ($property.SubType -eq 1) { $property.Value = $property.SubTypeMax }
                } catch { }
            }
        }
        # BMP is the native WIA transfer format. The JVM normalizes it to JPEG
        # before preview/upload, also handling drivers that return another format.
        $image = $item.Transfer('{B96B3CAB-0728-11D3-9D7B-0000F81EF32E}')
        $image.SaveFile((Join-Path $PSScriptRoot 'scan.bmp'))
        $response = @{ status = 'ok' }
    }
} catch {
    $code = 'scanner_error'
    $exception = $_.Exception
    while ($null -ne $exception) {
        $hresult = '{0:X8}' -f $exception.HResult
        switch ($hresult) {
            '80040154' { $code = 'wia_unavailable' }
            '80210002' { $code = 'paper_jam' }
            '80210003' { $code = 'paper_empty' }
            '80210005' { $code = 'device_offline' }
            '80210006' { $code = 'device_busy' }
            '8021000A' { $code = 'device_busy' }
            '80210015' { $code = 'device_offline' }
            '80210016' { $code = 'cover_open' }
        }
        $exception = $exception.InnerException
    }
    $response = @{ status = 'error'; code = $code }
} finally {
    foreach ($value in @($image, $item, $device, $manager)) {
        if ($null -ne $value -and [System.Runtime.InteropServices.Marshal]::IsComObject($value)) {
            try { [void] [System.Runtime.InteropServices.Marshal]::FinalReleaseComObject($value) } catch { }
        }
    }
    $json = $response | ConvertTo-Json -Compress -Depth 5
    $temporaryResponse = Join-Path $PSScriptRoot 'result.json.tmp'
    [System.IO.File]::WriteAllText($temporaryResponse, $json, (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::Move($temporaryResponse, (Join-Path $PSScriptRoot 'result.json'))
}
