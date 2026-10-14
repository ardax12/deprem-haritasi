-- Migration V2: Kandilli (KOERI) support, multi-source event grouping and advanced matching

-- 1. Source report revision enhancements for KOERI multi-magnitude and solution quality
ALTER TABLE source_report_revision ADD COLUMN IF NOT EXISTS solution_quality VARCHAR(64);
ALTER TABLE source_report_revision ADD COLUMN IF NOT EXISTS mag_md DOUBLE PRECISION;
ALTER TABLE source_report_revision ADD COLUMN IF NOT EXISTS mag_ml DOUBLE PRECISION;
ALTER TABLE source_report_revision ADD COLUMN IF NOT EXISTS mag_mw DOUBLE PRECISION;

-- 2. Enhance event_match with decision lifecycle, scores and revision references
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'CANDIDATE' NOT NULL;
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS score DOUBLE PRECISION;
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS depth_delta_km DOUBLE PRECISION;
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS magnitude_delta DOUBLE PRECISION;
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS decision_type VARCHAR(32) DEFAULT 'AUTO';
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS decision_reason VARCHAR(512);
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS left_revision_id UUID;
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS right_revision_id UUID;
ALTER TABLE event_match ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 3. Event Group (Unified seismic physical event)
CREATE TABLE IF NOT EXISTS event_group (
    id UUID PRIMARY KEY,
    title VARCHAR(256),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    preferred_magnitude_value DOUBLE PRECISION,
    preferred_magnitude_type VARCHAR(16),
    status VARCHAR(32) DEFAULT 'AUTO_CONFIRMED' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 4. Event Group Member (Report to event group membership)
CREATE TABLE IF NOT EXISTS event_group_member (
    id UUID PRIMARY KEY,
    event_group_id UUID NOT NULL,
    source_report_id UUID NOT NULL,
    source_revision_id UUID,
    membership_type VARCHAR(32) DEFAULT 'AUTO' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_group_member_group FOREIGN KEY (event_group_id) REFERENCES event_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_member_report FOREIGN KEY (source_report_id) REFERENCES source_report(id) ON DELETE CASCADE,
    CONSTRAINT uq_group_member_report UNIQUE (source_report_id)
);

-- 5. Indexes for query and matching efficiency
CREATE INDEX IF NOT EXISTS idx_match_status ON event_match(status);
CREATE INDEX IF NOT EXISTS idx_match_reports ON event_match(left_report_id, right_report_id);
CREATE INDEX IF NOT EXISTS idx_event_group_occurred_at ON event_group(occurred_at);
CREATE INDEX IF NOT EXISTS idx_group_member_group ON event_group_member(event_group_id);
