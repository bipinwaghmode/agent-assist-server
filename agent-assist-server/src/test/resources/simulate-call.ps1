param(
    [string]$ContactId = "sim-contact-1",
    [string]$Scenario  = ".\call-scenario.jsonl",
    [string]$Stream    = "contact-lens-dev",
    [string]$Endpoint  = "http://localhost:4566",
    [int]$DelayMs      = 800
)

$env:AWS_ACCESS_KEY_ID     = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
$env:AWS_DEFAULT_REGION    = "us-east-1"

$tmp  = [System.IO.Path]::GetTempFileName()
$utf8 = New-Object System.Text.UTF8Encoding($false)   # no BOM
$n = 0

foreach ($line in Get-Content $Scenario) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $json = $line.Replace("__CONTACT_ID__", $ContactId)
    [System.IO.File]::WriteAllText($tmp, $json, $utf8)
    aws --endpoint-url $Endpoint kinesis put-record --stream-name $Stream `
        --partition-key $ContactId --data "fileb://$tmp" | Out-Null
    $n++
    Write-Host "sent event $n"
    Start-Sleep -Milliseconds $DelayMs
}

Remove-Item $tmp
Write-Host "Done. Sent $n events for contact $ContactId"
