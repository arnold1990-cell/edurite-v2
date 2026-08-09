# Database Scaling

## Implemented

- HikariCP settings are configurable with `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_CONNECTION_TIMEOUT`, `DB_IDLE_TIMEOUT`, and `DB_MAX_LIFETIME`.
- Hikari metrics are exposed through Micrometer/Actuator for Prometheus scraping.
- Existing migrations already include many key indexes for auth, payments, notifications, institutions, school portal, psychometrics, curriculum, and learning resources.

## Risks Requiring Query Work

- Replace high-volume `findAll().stream().filter(...)` patterns with repository queries or projections.
- Add hard page-size caps to every collection endpoint.
- Use DTO projections for dashboards instead of loading full entities.
- Run `EXPLAIN ANALYZE` on production-like data before adding more indexes.
- Keep per-instance DB pool sizes conservative. At high replica counts, use RDS Proxy/PgBouncer-style pooling instead of increasing PostgreSQL `max_connections` excessively.

## Read Replica Preparation

The app is not yet wired to read replicas. A future step should introduce separate writer/read-only data sources and annotate read-heavy query services explicitly. Do not add replicas until infrastructure and consistency requirements are defined.
