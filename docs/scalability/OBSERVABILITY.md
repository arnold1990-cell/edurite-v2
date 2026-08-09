# Observability

## Implemented

- Actuator exposes `health`, `info`, and `metrics`; `prometheus` is configured for exposure when `micrometer-registry-prometheus` is available in the deployment build.
- Liveness and readiness probes are enabled at `/actuator/health/liveness` and `/actuator/health/readiness`.
- HTTP latency histograms and p50/p95/p99 percentiles are enabled for `http.server.requests`.
- Hikari, JVM, GC, CPU, thread, Redis, cache, and Tomcat metrics are available through Micrometer where supported by Spring Boot.
- `X-Request-Id` is accepted or generated and returned on every request; it is also placed into MDC as `requestId`.
- Custom metrics added:
  - `edurite.sse.connections`
  - `edurite.background.inflight`

## Required Dashboards

Track request rate, p50/p95/p99 latency, 4xx/5xx rate, JVM heap, GC pause time, threads, Tomcat busy threads, Hikari active/idle/pending connections, Redis latency/errors, cache hit ratio, background in-flight queue depth, and SSE connections.

## Validation Note

Adding `micrometer-registry-prometheus` could not be validated in this workspace because Maven dependency download failed with a local PKIX certificate trust error. CI or the build host should fix Java CA trust and add the registry before relying on `/actuator/prometheus`.
