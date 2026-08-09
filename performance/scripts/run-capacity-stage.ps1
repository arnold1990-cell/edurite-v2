param(
    [Parameter(Mandatory=$true)][int]$TargetVus,
    [Parameter(Mandatory=$true)][string]$BaseUrl,
    [int]$UserIdStart = 1,
    [int]$UsersPerGenerator = 500,
    [int]$GeneratorIndex = 1,
    [int]$GeneratorCount = 1,
    [string]$WarmupDuration = "5m",
    [string]$RampUpDuration = "30m",
    [string]$HoldDuration = "30m",
    [string]$RampDownDuration = "10m",
    [int]$MinThinkSeconds = 8,
    [int]$MaxThinkSeconds = 22,
    [switch]$AllowDistributedCapacityTest
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$safeBase = ($BaseUrl -replace '[^a-zA-Z0-9]+', '-').Trim('-')
$resultDir = Join-Path $repoRoot "performance\results\capacity\$stamp-${TargetVus}vu-g${GeneratorIndex}of${GeneratorCount}-$safeBase"
New-Item -ItemType Directory -Force -Path $resultDir | Out-Null

$summaryJson = Join-Path $resultDir "k6-summary.json"
$stdout = Join-Path $resultDir "k6.out.txt"
$stderr = Join-Path $resultDir "k6.err.txt"
$envFile = Join-Path $resultDir "environment.txt"

@(
    "timestamp=$stamp",
    "baseUrl=$BaseUrl",
    "targetVus=$TargetVus",
    "userIdStart=$UserIdStart",
    "usersPerGenerator=$UsersPerGenerator",
    "generatorIndex=$GeneratorIndex",
    "generatorCount=$GeneratorCount",
    "warmupDuration=$WarmupDuration",
    "rampUpDuration=$RampUpDuration",
    "holdDuration=$HoldDuration",
    "rampDownDuration=$RampDownDuration",
    "minThinkSeconds=$MinThinkSeconds",
    "maxThinkSeconds=$MaxThinkSeconds",
    "machine=$env:COMPUTERNAME",
    "os=$([System.Environment]::OSVersion.VersionString)"
) | Set-Content $envFile

$allow = if ($AllowDistributedCapacityTest) { "true" } else { "false" }

$k6Args = @(
    "run",
    "--summary-export", $summaryJson,
    "-e", "BASE_URL=$BaseUrl",
    "-e", "TARGET_VUS=$TargetVus",
    "-e", "USER_ID_START=$UserIdStart",
    "-e", "USERS_PER_GENERATOR=$UsersPerGenerator",
    "-e", "GENERATOR_INDEX=$GeneratorIndex",
    "-e", "GENERATOR_COUNT=$GeneratorCount",
    "-e", "WARMUP_DURATION=$WarmupDuration",
    "-e", "RAMP_UP_DURATION=$RampUpDuration",
    "-e", "HOLD_DURATION=$HoldDuration",
    "-e", "RAMP_DOWN_DURATION=$RampDownDuration",
    "-e", "MIN_THINK_SECONDS=$MinThinkSeconds",
    "-e", "MAX_THINK_SECONDS=$MaxThinkSeconds",
    "-e", "ALLOW_DISTRIBUTED_CAPACITY_TEST=$allow",
    "performance/scripts/capacity-500k-steady-state.js"
)

$k6 = Start-Process -FilePath "k6" `
    -ArgumentList $k6Args `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -PassThru `
    -Wait `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr

"k6ExitCode=$($k6.ExitCode)" | Add-Content $envFile

Write-Output "RESULT_DIR=$resultDir"
Write-Output "K6_EXIT=$($k6.ExitCode)"
exit $k6.ExitCode
