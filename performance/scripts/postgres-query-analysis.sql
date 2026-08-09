-- PostgreSQL diagnostics for EduRite performance runs.
-- Run before, during, and immediately after k6 tests against the same database.

\pset pager off

select now() as captured_at;

select
    count(*) as total_connections,
    count(*) filter (where state = 'active') as active_queries,
    count(*) filter (where wait_event is not null) as waiting_backends,
    count(*) filter (where state = 'idle in transaction') as idle_in_transaction
from pg_stat_activity;

select
    state,
    wait_event_type,
    wait_event,
    count(*) as backends
from pg_stat_activity
group by state, wait_event_type, wait_event
order by backends desc, state nulls last, wait_event_type nulls last;

select
    pid,
    state,
    wait_event_type,
    wait_event,
    now() - query_start as query_age,
    left(regexp_replace(query, '\s+', ' ', 'g'), 500) as query
from pg_stat_activity
where datname = current_database()
  and state = 'active'
order by query_age desc
limit 20;

select
    datname,
    numbackends,
    xact_commit,
    xact_rollback,
    blks_read,
    blks_hit,
    round(100.0 * blks_hit / nullif(blks_hit + blks_read, 0), 2) as cache_hit_pct,
    temp_files,
    temp_bytes,
    deadlocks
from pg_stat_database
where datname = current_database();

select
    schemaname,
    relname,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_live_tup,
    n_dead_tup
from pg_stat_user_tables
order by seq_tup_read desc
limit 25;

select
    schemaname,
    relname,
    indexrelname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
from pg_stat_user_indexes
order by idx_scan desc
limit 25;

select
    case
        when exists (select 1 from pg_extension where extname = 'pg_stat_statements')
        then 'true'
        else 'false'
    end as has_pg_stat_statements
\gset

\if :has_pg_stat_statements
select
    calls,
    round(total_exec_time::numeric, 2) as total_exec_ms,
    round(mean_exec_time::numeric, 2) as mean_exec_ms,
    round(max_exec_time::numeric, 2) as max_exec_ms,
    rows,
    left(regexp_replace(query, '\s+', ' ', 'g'), 700) as query
from pg_stat_statements
order by total_exec_time desc
limit 25;
\else
select 'pg_stat_statements is not installed; skipping normalized query timing report.' as pg_stat_statements_status;
\endif
