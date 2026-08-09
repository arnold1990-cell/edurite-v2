# EduRite 500,000-VU Capacity Test Plan

This plan defines 500,000 VUs as 500,000 concurrent simulated users, not 500,000 total HTTP requests.

## Safety

- Do not run 500,000 VUs from a developer laptop.
- Do not run this against production.
- Do not include uncontrolled paid AI, email, SMS, or external API traffic.
- Do not generate 500,000 simultaneous logins.
- Use only non-production synthetic users and disposable test data.

## Workload Model

- 70% learners
- 15% teachers
- 8% school administrators
- 4% district/circuit/curriculum users
- 2% career-guidance-heavy users
- 1% administrators/other roles

The steady-state test uses authenticated sessions with token reuse. Login is performed as each VU starts during a ramp, not as a synchronized burst. Token refresh is only used when expiry requires it.

Default think time is 8-22 seconds, averaging roughly 15 seconds. With an average of one API request every 15 seconds, 500,000 concurrent users implies about 33,333 requests/second. The final request rate must be measured from k6 output, not inferred.

## Capacity Ladder

Run stages in order and stop on the first unhealthy stage:

500, 1,000, 2,500, 5,000, 10,000, 25,000, 50,000, 100,000, 200,000, 300,000, 400,000, 500,000 VUs.

The 50,000+ stages require distributed load generators and `ALLOW_DISTRIBUTED_CAPACITY_TEST=true`.

## Distributed Generator Partitioning

Each generator must receive a unique synthetic-user range.

Example for 10 generators with 10,000 users each:

```powershell
powershell -File performance/scripts/run-capacity-stage.ps1 `
  -TargetVus 10000 `
  -BaseUrl https://perf.edurite.example `
  -UserIdStart 1 `
  -UsersPerGenerator 10000 `
  -GeneratorIndex 1 `
  -GeneratorCount 10 `
  -AllowDistributedCapacityTest
```

Generator 2 uses `-UserIdStart 10001`, generator 3 uses `20001`, and so on.

## Required Monitoring

For every stage collect:

- k6 summary JSON and dropped iterations.
- Per-generator CPU, RAM, network, open sockets.
- Load balancer requests/sec, active connections, upstream errors, 502/503/504.
- Per-backend CPU, memory, JVM heap, GC, Tomcat busy threads, request rate.
- Hikari active, idle, pending, acquisition latency.
- PostgreSQL CPU, memory, active queries, wait events, locks, slow queries, cache hit ratio.
- Redis connected clients, ops/sec, CPU, memory, latency, evictions, blocked clients.

## Stop Conditions

Stop escalation when any of these occur:

- HTTP failure rate exceeds the stage threshold.
- Ordinary API p95 exceeds threshold significantly.
- p99 grows without recovery.
- Load generator CPU/RAM/network saturates.
- Backend instability or repeated restarts occur.
- Hikari pending remains non-zero under steady state.
- PostgreSQL or Redis becomes unstable.
- Load balancer starts returning/refusing significant traffic.

## Success Statement Required

A successful 500,000-VU result must state:

`EduRite sustained 500,000 concurrent virtual users in the tested staging environment for [duration], processing [X] requests/second with p95 [Y], p99 [Z], and [N]% HTTP failures under the defined workload.`

If a lower stage is the highest healthy stage, state that lower stage instead. Do not extrapolate.
