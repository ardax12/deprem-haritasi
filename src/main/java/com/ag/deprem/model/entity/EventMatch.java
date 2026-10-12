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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
