-- Migration V3: Event match uniqueness, check constraints, and performance indexes

-- 1. Ensure event_match has no self-matches or duplicate pairs
ALTER TABLE event_match ADD CONSTRAINT chk_event_match_distinct_reports CHECK (left_report_id <> right_report_id);

-- 2. Enforce uniqueness on matched report pair (left_report_id, right_report_id)
ALTER TABLE event_match ADD CONSTRAINT uq_event_match_reports UNIQUE (left_report_id, right_report_id);

-- 3. Composite index on source_report_revision for fast spatio-temporal matching queries
CREATE INDEX IF NOT EXISTS idx_revision_report_occurred ON source_report_revision(occurred_at, latitude, longitude);
