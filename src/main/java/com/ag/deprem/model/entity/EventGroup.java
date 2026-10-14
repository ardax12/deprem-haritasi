package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_group")
public class EventGroup {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "preferred_magnitude_value")
    private Double preferredMagnitudeValue;

    @Column(name = "preferred_magnitude_type", length = 16)
    private String preferredMagnitudeType;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "AUTO_CONFIRMED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "eventGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventGroupMember> members = new ArrayList<>();

    public EventGroup() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getPreferredMagnitudeValue() {
        return preferredMagnitudeValue;
    }

    public void setPreferredMagnitudeValue(Double preferredMagnitudeValue) {
        this.preferredMagnitudeValue = preferredMagnitudeValue;
    }

    public String getPreferredMagnitudeType() {
        return preferredMagnitudeType;
    }

    public void setPreferredMagnitudeType(String preferredMagnitudeType) {
        this.preferredMagnitudeType = preferredMagnitudeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<EventGroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<EventGroupMember> members) {
        this.members = members;
    }
}
