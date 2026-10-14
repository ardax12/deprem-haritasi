package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_group_member", uniqueConstraints = {
    @UniqueConstraint(name = "uq_group_member_report", columnNames = {"source_report_id"})
})
public class EventGroupMember {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_group_id", nullable = false)
    private EventGroup eventGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_report_id", nullable = false)
    private SourceReport sourceReport;

    @Column(name = "source_revision_id")
    private UUID sourceRevisionId;

    @Column(name = "membership_type", nullable = false, length = 32)
    private String membershipType = "AUTO";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public EventGroupMember() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EventGroup getEventGroup() {
        return eventGroup;
    }

    public void setEventGroup(EventGroup eventGroup) {
        this.eventGroup = eventGroup;
    }

    public SourceReport getSourceReport() {
        return sourceReport;
    }

    public void setSourceReport(SourceReport sourceReport) {
        this.sourceReport = sourceReport;
    }

    public UUID getSourceRevisionId() {
        return sourceRevisionId;
    }

    public void setSourceRevisionId(UUID sourceRevisionId) {
        this.sourceRevisionId = sourceRevisionId;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
