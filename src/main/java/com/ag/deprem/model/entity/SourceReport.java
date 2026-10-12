package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "source_report", uniqueConstraints = {
    @UniqueConstraint(name = "uq_source_event", columnNames = {"source", "source_event_id"})
})
public class SourceReport {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_event_id", nullable = false, length = 64)
    private String sourceEventId;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "current_revision_id")
    private UUID currentRevisionId;

    @Column(name = "is_event_update")
    private Boolean isEventUpdate = false;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

    @OneToMany(mappedBy = "sourceReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SourceReportRevision> revisions = new ArrayList<>();

    public SourceReport() {
    }

    public SourceReport(UUID id, String source, String sourceEventId, Instant firstSeenAt, Instant lastSeenAt) {
        this.id = id;
        this.source = source;
        this.sourceEventId = sourceEventId;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public UUID getCurrentRevisionId() {
        return currentRevisionId;
    }

    public void setCurrentRevisionId(UUID currentRevisionId) {
        this.currentRevisionId = currentRevisionId;
    }

    public Boolean getIsEventUpdate() {
        return isEventUpdate;
    }

    public void setIsEventUpdate(Boolean isEventUpdate) {
        this.isEventUpdate = isEventUpdate;
    }

    public Instant getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public void setSourceUpdatedAt(Instant sourceUpdatedAt) {
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public List<SourceReportRevision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<SourceReportRevision> revisions) {
        this.revisions = revisions;
    }
}
