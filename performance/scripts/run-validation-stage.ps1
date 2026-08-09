param(
    [string]$Name = "smoke",
    [string]$BaseUrl = "http://localhost:5173",
    [int]$Vus = 5,
    [string]$Duration = "30s",
    [int]$PerfTestUserCount = 100,
    [string]$Script = "performance/scripts/api-journey.js"
)

$ErrorActionPreference = "Stop"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = Join-Path "performance/results" "$timestamp-$Name"
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

docker ps --format "{{.Names}} {{.Status}} {{.Ports}}" |
    Select-String "edurite" |
    Set-Content -Path (Join-Path $resultDir "docker-containers-before.txt")

docker stats --no-stream --format "{{.Name}} cpu={{.CPUPerc}} mem={{.MemUsage}}" |
    Select-String "edurite" |
    Set-Content -Path (Join-Path $resultDir "docker-stats-before.txt")

docker exec edurite-postgres psql -U postgres -d edurite -tAc "SHOW max_connections; SELECT count(*) FILTER (WHERE state = 'active') AS active, count(*) FILTER (WHERE wait_event_type IS NOT NULL AND wait_event_type <> 'Client') AS non_client_waiting, count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_tx, count(*) AS total FROM pg_stat_activity;" |
    Set-Content -Path (Join-Path $resultDir "postgres-before.txt")

docker exec edurite-redis redis-cli INFO clients |
    Select-String "connected_clients|maxclients|blocked_clients" |
    Set-Content -Path (Join-Path $resultDir "redis-before.txt")

$env:BASE_URL = $BaseUrl
$env:VUS = [string]$Vus
$env:DURATION = $Duration
$env:PERF_TEST_USER_COUNT = [string]$PerfTestUserCount

$k6Out = Join-Path $resultDir "k6.out.txt"
$k6Err = Join-Path $resultDir "k6.err.txt"
$k6Args = @(
    "run",
    "-e", "BASE_URL=$BaseUrl",
    "-e", "VUS=$Vus",
    "-e", "DURATION=$Duration",
    "-e", "PERF_TEST_USER_COUNT=$PerfTestUserCount",
    $Script
)
$process = Start-Process -FilePath "k6" -ArgumentList $k6Args -NoNewWindow -Wait -PassThru -RedirectStandardOutput $k6Out -RedirectStandardError $k6Err
$k6ExitCode = $process.ExitCode

docker stats --no-stream --format "{{.Name}} cpu={{.CPUPerc}} mem={{.MemUsage}}" |
    Select-String "edurite" |
    Set-Content -Path (Join-Path $resultDir "docker-stats-after.txt")

docker exec edurite-postgres psql -U postgres -d edurite -tAc "SELECT count(*) FILTER (WHERE state = 'active') AS active, count(*) FILTER (WHERE wait_event_type IS NOT NULL AND wait_event_type <> 'Client') AS non_client_waiting, count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_tx, count(*) AS total FROM pg_stat_activity;" |
    Set-Content -Path (Join-Path $resultDir "postgres-after.txt")

docker exec edurite-redis redis-cli INFO clients |
    Select-String "connected_clients|maxclients|blocked_clients" |
    Set-Content -Path (Join-Path $resultDir "redis-after.txt")

Write-Output "Results written to $resultDir"
if ($k6ExitCode -ne 0) {
    throw "k6 exited with code $k6ExitCode. See $k6Out and $k6Err"
}
