package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "archive_manifest")
public class ArchiveManifest {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "archive_version", nullable = false, length = 32)
    private String archiveVersion;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "record_count", nullable = false)
    private Long recordCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "schema_version", nullable = false, length = 32)
    private String schemaVersion;

    @Column(name = "source_coverage", nullable = false, length = 64)
    private String sourceCoverage;

    @Column(name = "verified")
    private Boolean verified = false;

    public ArchiveManifest() {
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
}
