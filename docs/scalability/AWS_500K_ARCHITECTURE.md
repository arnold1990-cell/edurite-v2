# AWS 500K Reference Architecture

```
Users
  |
Route 53
  |
CloudFront + AWS WAF
  |
Application Load Balancer
  |
ECS/EKS backend service ---- ElastiCache Redis
  |        |                 |
  |        +---- SQS workers |
  |                          |
Aurora/RDS PostgreSQL -- RDS Proxy
  |
S3 for uploads/static objects

Metrics/logs: CloudWatch + Prometheus/Grafana
```

## Components

- Route 53: DNS and health-aware routing.
- CloudFront: static frontend delivery, edge caching, TLS, and origin shielding.
- AWS WAF: coarse traffic filtering and infrastructure-level rate limits.
- ALB: HTTP routing, readiness health checks, connection reuse.
- ECS or EKS: horizontally scaled backend containers.
- Aurora/RDS PostgreSQL: durable relational store; Aurora only if measured workload justifies it.
- RDS Proxy: connection pooling when backend replica count would otherwise exhaust PostgreSQL.
- ElastiCache Redis: distributed rate limits, cache, and temporary coordination.
- S3: durable object storage for uploads and generated files.
- SQS: background task buffer for emails, AI jobs, file processing, and retries.
- CloudWatch/Prometheus/Grafana: metrics, logs, alerting, dashboards.

This is a reference architecture, not proof that 500,000 users have been verified.
