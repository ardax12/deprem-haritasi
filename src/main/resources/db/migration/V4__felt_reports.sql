-- Migration V4: "Did You Feel It?" (Hissettiniz mi?) crowdsourcing table and indexes

CREATE TABLE IF NOT EXISTS felt_report (
    id UUID PRIMARY KEY,
    source_report_id UUID NOT NULL REFERENCES source_report(id) ON DELETE CASCADE,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_hash VARCHAR(64) NOT NULL,
    felt_level VARCHAR(20) NOT NULL,
    motion_type VARCHAR(30),
    reaction VARCHAR(30),
    sound_heard BOOLEAN,
    damage_observed VARCHAR(30),
    building_floor INT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    province VARCHAR(64),
    district VARCHAR(64),
    calculated_intensity INT NOT NULL CHECK (calculated_intensity BETWEEN 1 AND 10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Enforce 1 submission per client per earthquake event
CREATE UNIQUE INDEX IF NOT EXISTS idx_felt_report_ip_event ON felt_report(source_report_id, ip_hash);

-- Fast lookup indexes for aggregation queries
CREATE INDEX IF NOT EXISTS idx_felt_report_event_intensity ON felt_report(source_report_id, calculated_intensity);
CREATE INDEX IF NOT EXISTS idx_felt_report_province ON felt_report(source_report_id, province);
