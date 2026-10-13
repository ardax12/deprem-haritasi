-- Initial schema for Turkiye Deprem Izleme Haritasi

CREATE TABLE IF NOT EXISTS source_report (
    id UUID PRIMARY KEY,
    source VARCHAR(32) NOT NULL,
    source_event_id VARCHAR(64) NOT NULL,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_revision_id UUID,
    is_event_update BOOLEAN DEFAULT FALSE,
    source_updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_source_event UNIQUE (source, source_event_id)
);

CREATE TABLE IF NOT EXISTS source_report_revision (
    id UUID PRIMARY KEY,
    source_report_id UUID NOT NULL,
    revision_number INT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    depth_km DOUBLE PRECISION,
    magnitude_value DOUBLE PRECISION,
    magnitude_type VARCHAR(16),
    location_text VARCHAR(512),
    country VARCHAR(128),
    province VARCHAR(128),
    district VARCHAR(128),
    neighborhood VARCHAR(128),
    source_payload_hash VARCHAR(64) NOT NULL,
    normalized_hash VARCHAR(64) NOT NULL,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    raw_payload_ref VARCHAR(512),
    CONSTRAINT fk_revision_report FOREIGN KEY (source_report_id) REFERENCES source_report(id) ON DELETE CASCADE,
    CONSTRAINT uq_report_revision UNIQUE (source_report_id, revision_number)
);

CREATE TABLE IF NOT EXISTS event_match (
    id UUID PRIMARY KEY,
    left_report_id UUID NOT NULL,
    right_report_id UUID NOT NULL,
    algorithm_version VARCHAR(32) NOT NULL,
    time_delta_seconds BIGINT NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_match_left FOREIGN KEY (left_report_id) REFERENCES source_report(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_right FOREIGN KEY (right_report_id) REFERENCES source_report(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS archive_manifest (
    id UUID PRIMARY KEY,
    archive_version VARCHAR(32) NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    record_count BIGINT NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    source_coverage VARCHAR(64) NOT NULL,
    verified BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_report_source_event ON source_report(source, source_event_id);
CREATE INDEX IF NOT EXISTS idx_revision_occurred_at ON source_report_revision(occurred_at);
CREATE INDEX IF NOT EXISTS idx_revision_lat_lon ON source_report_revision(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_revision_magnitude ON source_report_revision(magnitude_value);
CREATE INDEX IF NOT EXISTS idx_revision_magnitude_type ON source_report_revision(magnitude_type);
