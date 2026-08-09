param(
    [string]$Name = "phase8",
    [string]$BaseUrl = "http://localhost:5173",
    [int]$Vus = 100,
    [string]$Duration = "1m",
    [int]$PerfTestUserCount = 50000,
    [string]$Script = "performance/scripts/api-journey.js",
    [int]$SampleSeconds = 2
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = Join-Path $repoRoot "performance\results\$stamp-$Name"
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$k6Out = Join-Path $resultDir "k6.out.txt"
$k6Err = Join-Path $resultDir "k6.err.txt"
$summaryJson = Join-Path $resultDir "k6-summary.json"
$dockerStats = Join-Path $resultDir "docker-stats.ndjson"
$backendMetrics = Join-Path $resultDir "backend-metrics.ndjson"
$postgresMetrics = Join-Path $resultDir "postgres-samples.txt"
$redisMetrics = Join-Path $resultDir "redis-samples.txt"
$hostMetrics = Join-Path $resultDir "host-samples.txt"
$containerState = Join-Path $resultDir "container-state.txt"

docker ps --format "{{.Names}} {{.Status}} {{.Ports}}" | Set-Content $containerState
docker inspect edurite-frontend --format "frontend restart={{.RestartCount}} oom={{.State.OOMKilled}}" | Add-Content $containerState
docker inspect edurite-ai-career-guidance-clean-backend-1 --format "backend1 restart={{.RestartCount}} oom={{.State.OOMKilled}}" | Add-Content $containerState
docker inspect edurite-ai-career-guidance-clean-backend-2 --format "backend2 restart={{.RestartCount}} oom={{.State.OOMKilled}}" | Add-Content $containerState

$metricNames = @(
    "hikaricp.connections.active",
    "hikaricp.connections.idle",
    "hikaricp.connections.pending",
    "hikaricp.connections.max",
    "hikaricp.connections.timeout",
    "hikaricp.connections.acquire",
    "hikaricp.connections.usage",
    "tomcat.threads.current",
    "tomcat.threads.busy",
    "tomcat.threads.config.max",
    "process.cpu.usage",
    "jvm.memory.used",
    "jvm.gc.pause",
    "jvm.threads.live"
)

$k6Args = @(
    "run",
    "--summary-export", $summaryJson,
    "-e", "BASE_URL=$BaseUrl",
    "-e", "VUS=$Vus",
    "-e", "DURATION=$Duration",
    "-e", "PERF_TEST_USER_COUNT=$PerfTestUserCount",
    $Script
)

$k6 = Start-Process -FilePath "k6" `
    -ArgumentList $k6Args `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -PassThru `
    -RedirectStandardOutput $k6Out `
    -RedirectStandardError $k6Err

while (-not $k6.HasExited) {
    $now = Get-Date -Format o

    docker stats --no-stream --format "{{json .}}" | Select-String "edurite" | ForEach-Object {
        [pscustomobject]@{ capturedAt = $now; dockerStats = $_.Line | ConvertFrom-Json } | ConvertTo-Json -Compress -Depth 8
    } | Add-Content $dockerStats

    foreach ($container in @("edurite-ai-career-guidance-clean-backend-1", "edurite-ai-career-guidance-clean-backend-2")) {
        foreach ($metric in $metricNames) {
            try {
                $raw = docker exec $container curl -fsS "http://127.0.0.1:8080/actuator/metrics/$metric" 2>$null
                [pscustomobject]@{
                    capturedAt = $now
                    container = $container
                    metric = $metric
                    data = $raw | ConvertFrom-Json
                } | ConvertTo-Json -Compress -Depth 8 | Add-Content $backendMetrics
            } catch {
                [pscustomobject]@{
                    capturedAt = $now
                    container = $container
                    metric = $metric
                    error = $_.Exception.Message
                } | ConvertTo-Json -Compress | Add-Content $backendMetrics
            }
        }
    }

    "==== $now postgres ====" | Add-Content $postgresMetrics
    docker exec edurite-postgres psql -U postgres -d edurite -v ON_ERROR_STOP=0 -c "select count(*) as total, count(*) filter (where state='active') as active, count(*) filter (where wait_event is not null and wait_event_type <> 'Client') as waiting, count(*) filter (where state='idle in transaction') as idle_in_xact from pg_stat_activity where datname=current_database();" | Add-Content $postgresMetrics
    docker exec edurite-postgres psql -U postgres -d edurite -v ON_ERROR_STOP=0 -c "select state, wait_event_type, wait_event, count(*) from pg_stat_activity where datname=current_database() group by 1,2,3 order by 4 desc;" | Add-Content $postgresMetrics
    docker exec edurite-postgres psql -U postgres -d edurite -v ON_ERROR_STOP=0 -c "select now()-query_start as age, state, wait_event_type, wait_event, left(regexp_replace(query,'\s+',' ','g'),180) as query from pg_stat_activity where datname=current_database() and state='active' order by age desc limit 8;" | Add-Content $postgresMetrics

    "==== $now redis ====" | Add-Content $redisMetrics
    docker exec edurite-redis redis-cli INFO clients | Select-String "connected_clients|maxclients|blocked_clients" | Add-Content $redisMetrics
    docker exec edurite-redis redis-cli INFO stats | Select-String "instantaneous_ops_per_sec|rejected_connections|total_error_replies" | Add-Content $redisMetrics
    docker exec edurite-redis redis-cli INFO memory | Select-String "used_memory_human|used_memory_peak_human" | Add-Content $redisMetrics

    "==== $now host ====" | Add-Content $hostMetrics
    Get-CimInstance Win32_Processor | Select-Object -First 1 LoadPercentage | Format-List | Out-String | Add-Content $hostMetrics
    Get-CimInstance Win32_OperatingSystem | Select-Object FreePhysicalMemory,TotalVisibleMemorySize | Format-List | Out-String | Add-Content $hostMetrics
    Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue |
        Group-Object State |
        Sort-Object Name |
        ForEach-Object { "$($_.Name)=$($_.Count)" } |
        Add-Content $hostMetrics

    Start-Sleep -Seconds $SampleSeconds
}

$k6.WaitForExit()

docker inspect edurite-frontend --format "frontend restart={{.RestartCount}} oom={{.State.OOMKilled}}" | Add-Content $containerState
docker inspect edurite-ai-career-guidance-clean-backend-1 --format "backend1 restart={{.RestartCount}} oom={{.State.OOMKilled}}" | Add-Content $containerState
docker inspect edurite-ai-career-guidance-clean-backend-2 --format "backend2 restart={{.RestartCount}} oom={{.State.OOMKilled}}" | Add-Content $containerState

"RESULT_DIR=$resultDir"
"K6_EXIT=$($k6.ExitCode)"
Get-Content $k6Out -Tail 140
if (Test-Path $k6Err) {
    Get-Content $k6Err -Tail 40
}

exit $k6.ExitCode
