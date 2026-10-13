package com.ag.deprem.model.dto;

import java.time.Instant;
import java.util.UUID;

public class ArchiveManifestDto {
    private UUID id;
    private String archiveVersion;
    private Instant startAt;
    private Instant endAt;
    private Long recordCount;
    private Instant generatedAt;
    private String checksum;
    private String schemaVersion;
    private String sourceCoverage;
    private Boolean verified;
    private String downloadUrl;

    public ArchiveManifestDto() {
    }

    public ArchiveManifestDto(UUID id,
                              String archiveVersion,
                              Instant startAt,
                              Instant endAt,
                              Long recordCount,
                              Instant generatedAt,
                              String checksum,
                              String schemaVersion,
                              String sourceCoverage,
                              Boolean verified,
                              String downloadUrl) {
        this.id = id;
        this.archiveVersion = archiveVersion;
        this.startAt = startAt;
        this.endAt = endAt;
        this.recordCount = recordCount;
        this.generatedAt = generatedAt;
        this.checksum = checksum;
        this.schemaVersion = schemaVersion;
        this.sourceCoverage = sourceCoverage;
        this.verified = verified;
        this.downloadUrl = downloadUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getArchiveVersion() {
        return archiveVersion;
    }

    public void setArchiveVersion(String archiveVersion) {
        this.archiveVersion = archiveVersion;
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

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getSourceCoverage() {
        return sourceCoverage;
    }

    public void setSourceCoverage(String sourceCoverage) {
        this.sourceCoverage = sourceCoverage;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
