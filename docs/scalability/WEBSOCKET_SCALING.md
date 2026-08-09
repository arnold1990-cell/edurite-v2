# Live Connection Scaling

No STOMP WebSocket implementation was found. The app uses SSE for notification streams.

## Current State

- Endpoint: `/api/v1/notifications/stream` and `/api/notifications/stream`.
- Auth: JWT via `Authorization: Bearer` or `access_token` query parameter for stream clients.
- State: active `SseEmitter` objects are kept in local JVM memory.

## Implemented

- SSE timeout is now bounded and configurable with `edurite.notifications.sse.timeout-ms`.
- Active local SSE connections are exposed as `edurite.sse.connections`.

## 500K Architecture Requirement

One Spring Boot JVM must not be treated as the owner of all live connections. For 500,000 active live clients, use one of:

- Managed real-time gateway service plus backend event publishing.
- Dedicated notification gateway tier backed by Redis Pub/Sub, NATS, Kafka, or RabbitMQ.
- Sticky load balancing only as a transitional mitigation, not a final architecture.

The current SSE implementation is not a verified 500,000 persistent-connection architecture.
