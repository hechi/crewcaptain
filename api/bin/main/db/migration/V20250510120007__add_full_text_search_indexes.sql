-- Full-text search support.
-- For MVP, we rely on query-time to_tsvector() calls which are efficient for
-- the expected data volume (single manager's data per query, scoped by user_id).
-- The existing user_id indexes ensure the query planner filters by user first,
-- then applies FTS on the small result set.
--
-- For future optimization at scale, GIN indexes can be added using either:
-- 1. Stored generated tsvector columns (PostgreSQL 12+)
-- 2. Immutable wrapper functions
-- 3. Materialized views with tsvector columns
--
-- This migration is intentionally a no-op placeholder to document the decision
-- and reserve the migration slot. The search feature works without GIN indexes
-- because all queries are scoped by user_id (which has B-tree indexes) first.

-- No schema changes needed — search uses existing tables and indexes.
SELECT 1;
