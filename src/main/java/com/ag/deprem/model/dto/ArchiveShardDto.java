package com.ag.deprem.model.dto;

import java.time.Instant;
import java.util.List;

public class ArchiveShardDto {
    private String schemaVersion;
    private String archiveVersion;
    private Instant generatedAt;
    private Instant startAt;
    private Instant endAt;
    private String sourceCoverage;
    private int recordCount;
    private List<EarthquakeItemDto> items;

    public ArchiveShardDto() {
    }

    public ArchiveShardDto(String schemaVersion,
                           String archiveVersion,
                           Instant generatedAt,
                           Instant startAt,
                           Instant endAt,
                           String sourceCoverage,
                           int recordCount,
                           List<EarthquakeItemDto> items) {
        this.schemaVersion = schemaVersion;
        this.archiveVersion = archiveVersion;
        this.generatedAt = generatedAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.sourceCoverage = sourceCoverage;
        this.recordCount = recordCount;
        this.items = items;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getArchiveVersion() {
        return archiveVersion;
    }

    public void setArchiveVersion(String archiveVersion) {
        this.archiveVersion = archiveVersion;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public String getSourceCoverage() {
        return sourceCoverage;
    }

    public void setSourceCoverage(String sourceCoverage) {
        this.sourceCoverage = sourceCoverage;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public List<EarthquakeItemDto> getItems() {
        return items;
    }

    public void setItems(List<EarthquakeItemDto> items) {
        this.items = items;
    }
}
