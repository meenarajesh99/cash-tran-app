[CmdletBinding()]
param(
    [uri]$BackendUrl = "https://cashtran-backend.onrender.com",
    [ValidateRange(5, 1800)]
    [int]$DurationSeconds = 300,
    [ValidateRange(5, 60)]
    [int]$IntervalSeconds = 5,
    [ValidateRange(1, 30)]
    [int]$TimeoutSeconds = 10
)

if (-not $BackendUrl.IsAbsoluteUri -or $BackendUrl.Scheme -notin @("http", "https")) {
    throw "BackendUrl must be an absolute HTTP or HTTPS URL."
}
if ($BackendUrl.UserInfo -or $BackendUrl.Query -or $BackendUrl.Fragment) {
    throw "BackendUrl must not include credentials, a query string, or a fragment."
}

$baseUrl = $BackendUrl.AbsoluteUri.TrimEnd("/")
$paths = @(
    "/",
    "/actuator/health",
    "/api/balance",
    "/api/transfers"
)
$endTime = (Get-Date).AddSeconds($DurationSeconds)
$statusCounts = @{}
$totalRequests = 0

Write-Host "Sending read-only GET traffic to $baseUrl for up to $DurationSeconds seconds."
Write-Host "Protected API routes are called without credentials and may return 401 or 403."

while ((Get-Date) -lt $endTime) {
    foreach ($path in $paths) {
        if ((Get-Date) -ge $endTime) {
            break
        }

        $requestUrl = "$baseUrl$path"
        $status = "ERR"

        try {
            $response = Invoke-WebRequest `
                -Uri $requestUrl `
                -Method Get `
                -TimeoutSec $TimeoutSeconds `
                -UseBasicParsing `
                -ErrorAction Stop
            $status = [string][int]$response.StatusCode
        }
        catch {
            $errorResponse = $_.Exception.Response
            if ($null -ne $errorResponse) {
                $status = [string][int]$errorResponse.StatusCode
            }
            else {
                Write-Warning "GET $path failed: $($_.Exception.Message)"
            }
        }

        if (-not $statusCounts.ContainsKey($status)) {
            $statusCounts[$status] = 0
        }
        $statusCounts[$status]++
        $totalRequests++
        Write-Host ("{0} {1} -> {2}" -f (Get-Date -Format "HH:mm:ss"), $path, $status)
    }

    $remainingSeconds = ($endTime - (Get-Date)).TotalSeconds
    if ($remainingSeconds -gt 0) {
        Start-Sleep -Seconds ([Math]::Min($IntervalSeconds, [Math]::Ceiling($remainingSeconds)))
    }
}

Write-Host ""
Write-Host "Completed $totalRequests request(s). Status summary:"
foreach ($status in ($statusCounts.Keys | Sort-Object)) {
    Write-Host ("  {0}: {1}" -f $status, $statusCounts[$status])
}
