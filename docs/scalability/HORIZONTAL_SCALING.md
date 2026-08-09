# Horizontal Scaling

## Implemented

- HTTP authentication is stateless JWT. Every replica must share `JWT_SECRET`.
- Graceful shutdown is enabled with `server.shutdown=graceful`.
- Liveness/readiness probes are enabled.
- Backend container runs as a non-root user.
- Hikari, Tomcat, async, scheduler, Redis, and rate-limit values are environment configurable.
- Kubernetes reference manifests are under `deploy/kubernetes`.

## Operational Rules

- Run multiple backend replicas behind a load balancer.
- Keep API nodes stateless.
- Do not store persistent uploads on local container filesystems.
- Run scheduled/background workers as a separately scaled role when jobs become expensive or must be singleton.
- Use Redis or an external broker for distributed temporary state.

## ECS/EKS Mapping

On ECS, map the backend deployment to an ECS service behind an ALB with target group health checks using readiness. On EKS, use the provided Deployment, Service, HPA, and PDB as the starting point.
