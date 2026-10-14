package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_match")
public class EventMatch {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_report_id", nullable = false)
    private SourceReport leftReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "right_report_id", nullable = false)
    private SourceReport rightReport;

    @Column(name = "algorithm_version", nullable = false, length = 32)
    private String algorithmVersion;

    @Column(name = "time_delta_seconds", nullable = false)
    private Long timeDeltaSeconds;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "CANDIDATE";

    @Column(name = "score")
    private Double score;

    @Column(name = "depth_delta_km")
    private Double depthDeltaKm;

    @Column(name = "magnitude_delta")
    private Double magnitudeDelta;

    @Column(name = "decision_type", length = 32)
    private String decisionType = "AUTO";

    @Column(name = "decision_reason", length = 512)
    private String decisionReason;

    @Column(name = "left_revision_id")
    private UUID leftRevisionId;

    @Column(name = "right_revision_id")
    private UUID rightRevisionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public EventMatch() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SourceReport getLeftReport() {
        return leftReport;
    }

    public void setLeftReport(SourceReport leftReport) {
        this.leftReport = leftReport;
    }

    public SourceReport getRightReport() {
        return rightReport;
    }

    public void setRightReport(SourceReport rightReport) {
        this.rightReport = rightReport;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }

    public Long getTimeDeltaSeconds() {
        return timeDeltaSeconds;
    }

    public void setTimeDeltaSeconds(Long timeDeltaSeconds) {
        this.timeDeltaSeconds = timeDeltaSeconds;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getDepthDeltaKm() {
        return depthDeltaKm;
    }

    public void setDepthDeltaKm(Double depthDeltaKm) {
        this.depthDeltaKm = depthDeltaKm;
    }

    public Double getMagnitudeDelta() {
        return magnitudeDelta;
    }

    public void setMagnitudeDelta(Double magnitudeDelta) {
        this.magnitudeDelta = magnitudeDelta;
    }

    public String getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(String decisionType) {
        this.decisionType = decisionType;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public UUID getLeftRevisionId() {
        return leftRevisionId;
    }

    public void setLeftRevisionId(UUID leftRevisionId) {
        this.leftRevisionId = leftRevisionId;
    }

    public UUID getRightRevisionId() {
        return rightRevisionId;
    }

    public void setRightRevisionId(UUID rightRevisionId) {
        this.rightRevisionId = rightRevisionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
