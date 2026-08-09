param(
    [string]$BaseUrl = "http://localhost:5173",
    [int]$Vus = 50,
    [string]$Duration = "2m",
    [int]$PerfTestUserCount = 100,
    [string]$BackendToStop = "edurite-ai-career-guidance-clean-backend-1"
)

$ErrorActionPreference = "Stop"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = Join-Path "performance/results" "$timestamp-failover-${Vus}vu"
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$k6Out = Join-Path $resultDir "k6.out.txt"
$k6Err = Join-Path $resultDir "k6.err.txt"
$k6Args = @(
    "run",
    "-e", "BASE_URL=$BaseUrl",
    "-e", "VUS=$Vus",
    "-e", "DURATION=$Duration",
    "-e", "PERF_TEST_USER_COUNT=$PerfTestUserCount",
    "performance/scripts/api-journey.js"
)

$process = Start-Process -FilePath "k6" -ArgumentList $k6Args -NoNewWindow -PassThru -RedirectStandardOutput $k6Out -RedirectStandardError $k6Err
Start-Sleep -Seconds 35

docker stop $BackendToStop | Set-Content -Path (Join-Path $resultDir "stopped-backend.txt")
Start-Sleep -Seconds 25

$counts = @{}
for ($i = 0; $i -lt 30; $i++) {
    $out = & curl.exe -s -D - -o NUL "$BaseUrl/actuator/health"
    $statusLine = $out | Select-Object -First 1
    $status = if ($statusLine -match "HTTP/\S+\s+(\d+)") { $Matches[1] } else { "unknown" }
    $headerLine = $out | Where-Object { $_ -match "^X-EduRite-Instance:" } | Select-Object -First 1
    $instance = if ($headerLine) { ($headerLine -replace "^X-EduRite-Instance:\s*", "").Trim() } else { "<missing>" }
    $key = "$status/$instance"
    if (-not $counts.ContainsKey($key)) {
        $counts[$key] = 0
    }
    $counts[$key]++
}

"during_failure_distribution=" + (($counts.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name):$($_.Value)" }) -join ",") |
    Set-Content -Path (Join-Path $resultDir "distribution-during-failure.txt")

$process.WaitForExit()
$k6ExitCode = $process.ExitCode

docker compose up -d --scale backend=2 backend frontend |
    Set-Content -Path (Join-Path $resultDir "restore.txt")

Start-Sleep -Seconds 20

docker ps --format "{{.Names}} {{.Status}}" |
    Select-String "edurite" |
    Set-Content -Path (Join-Path $resultDir "docker-after-restore.txt")

Write-Output "Results written to $resultDir"
if ($k6ExitCode -ne 0) {
    throw "k6 exited with code $k6ExitCode. See $k6Out and $k6Err"
}
