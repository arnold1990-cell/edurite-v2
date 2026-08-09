param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Vus = 100,
    [string]$Duration = "5m",
    [string]$PostgresContainer = "edurite-postgres",
    [string]$PostgresUser = "postgres",
    [string]$PostgresDatabase = "edurite"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = Join-Path $repoRoot "performance\results\run-$stamp"
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$k6Out = Join-Path $resultDir "k6.out.txt"
$k6Err = Join-Path $resultDir "k6.err.txt"
$metricsOut = Join-Path $resultDir "live-metrics.ndjson"
$pgOut = Join-Path $resultDir "postgres-samples.txt"
$statsOut = Join-Path $resultDir "docker-stats.txt"
$queryAnalysisOut = Join-Path $resultDir "postgres-query-analysis-during-load.txt"
$queryAnalysisScript = Join-Path $repoRoot "performance\scripts\postgres-query-analysis.sql"

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
    "system.cpu.usage",
    "jvm.memory.used",
    "jvm.gc.pause",
    "jvm.threads.live"
)

$k6Args = @(
    "run",
    "-e", "BASE_URL=$BaseUrl",
    "-e", "VUS=$Vus",
    "-e", "DURATION=$Duration",
    "performance/scripts/api-journey.js"
)

$k6 = Start-Process -FilePath "k6" `
    -ArgumentList $k6Args `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -PassThru `
    -RedirectStandardOutput $k6Out `
    -RedirectStandardError $k6Err

$ranQueryAnalysis = $false
while (-not $k6.HasExited) {
    $now = Get-Date -Format o

    foreach ($name in $metricNames) {
        try {
            $metric = Invoke-RestMethod -TimeoutSec 2 -Uri "$BaseUrl/actuator/metrics/$name"
            [pscustomobject]@{
                capturedAt = $now
                metric = $name
                measurements = $metric.measurements
                availableTags = $metric.availableTags
            } | ConvertTo-Json -Compress -Depth 8 | Add-Content -Path $metricsOut
        } catch {
            [pscustomobject]@{
                capturedAt = $now
                metric = $name
                error = $_.Exception.Message
            } | ConvertTo-Json -Compress | Add-Content -Path $metricsOut
        }
    }

    "==== $now docker stats ====" | Add-Content $statsOut
    docker stats --no-stream --format "{{.Name}} cpu={{.CPUPerc}} mem={{.MemUsage}} net={{.NetIO}} block={{.BlockIO}} pids={{.PIDs}}" edurite-backend $PostgresContainer | Add-Content $statsOut

    "==== $now pg activity ====" | Add-Content $pgOut
    docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDatabase -v ON_ERROR_STOP=0 -c "select count(*) as total, count(*) filter (where state='active') as active, count(*) filter (where wait_event is not null) as waiting, count(*) filter (where state='idle in transaction') as idle_in_xact from pg_stat_activity where datname=current_database();" | Add-Content $pgOut
    docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDatabase -v ON_ERROR_STOP=0 -c "select state, wait_event_type, wait_event, count(*) from pg_stat_activity where datname=current_database() group by 1,2,3 order by 4 desc;" | Add-Content $pgOut
    docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDatabase -v ON_ERROR_STOP=0 -c "select pid,state,wait_event_type,wait_event,now()-query_start as age,left(regexp_replace(query,'\s+',' ','g'),240) as query from pg_stat_activity where datname=current_database() and state='active' order by age desc limit 10;" | Add-Content $pgOut
    docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDatabase -v ON_ERROR_STOP=0 -c "select blocked.pid as blocked_pid, blocking.pid as blocking_pid, now()-blocked.query_start as blocked_age, left(blocked.query,160) as blocked_query from pg_stat_activity blocked join pg_locks blocked_locks on blocked_locks.pid=blocked.pid join pg_locks blocking_locks on blocking_locks.locktype=blocked_locks.locktype and blocking_locks.database is not distinct from blocked_locks.database and blocking_locks.relation is not distinct from blocked_locks.relation and blocking_locks.page is not distinct from blocked_locks.page and blocking_locks.tuple is not distinct from blocked_locks.tuple and blocking_locks.virtualxid is not distinct from blocked_locks.virtualxid and blocking_locks.transactionid is not distinct from blocked_locks.transactionid and blocking_locks.classid is not distinct from blocked_locks.classid and blocking_locks.objid is not distinct from blocked_locks.objid and blocking_locks.objsubid is not distinct from blocked_locks.objsubid and blocking_locks.pid <> blocked_locks.pid join pg_stat_activity blocking on blocking.pid=blocking_locks.pid where not blocked_locks.granted and blocking_locks.granted;" | Add-Content $pgOut

    if (-not $ranQueryAnalysis -and (Test-Path $queryAnalysisScript)) {
        "==== $now postgres-query-analysis.sql ====" | Add-Content $queryAnalysisOut
        Get-Content $queryAnalysisScript | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDatabase -v ON_ERROR_STOP=0 | Add-Content $queryAnalysisOut
        $ranQueryAnalysis = $true
    }

    Start-Sleep -Seconds 5
}

$k6.WaitForExit()

"RESULT_DIR=$resultDir"
"K6_EXIT=$($k6.ExitCode)"
Get-Content $k6Out -Tail 140
if (Test-Path $k6Err) {
    Get-Content $k6Err -Tail 60
}
