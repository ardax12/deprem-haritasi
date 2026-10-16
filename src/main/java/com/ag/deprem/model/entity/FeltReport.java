package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "felt_report", uniqueConstraints = {
    @UniqueConstraint(name = "idx_felt_report_ip_event", columnNames = {"source_report_id", "ip_hash"})
})
public class FeltReport {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_report_id", nullable = false)
    private SourceReport sourceReport;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "felt_level", nullable = false, length = 20)
    private String feltLevel;

    @Column(name = "motion_type", length = 30)
    private String motionType;

    @Column(name = "reaction", length = 30)
    private String reaction;

    @Column(name = "sound_heard")
    private Boolean soundHeard;

    @Column(name = "damage_observed", length = 30)
    private String damageObserved;

    @Column(name = "building_floor")
    private Integer buildingFloor;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "province", length = 64)
    private String province;

    @Column(name = "district", length = 64)
    private String district;

    @Column(name = "calculated_intensity", nullable = false)
    private Integer calculatedIntensity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FeltReport() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SourceReport getSourceReport() {
        return sourceReport;
    }

    public void setSourceReport(SourceReport sourceReport) {
        this.sourceReport = sourceReport;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(Instant reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    public String getFeltLevel() {
        return feltLevel;
    }

    public void setFeltLevel(String feltLevel) {
        this.feltLevel = feltLevel;
    }

    public String getMotionType() {
        return motionType;
    }

    public void setMotionType(String motionType) {
        this.motionType = motionType;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public Boolean getSoundHeard() {
        return soundHeard;
    }

    public void setSoundHeard(Boolean soundHeard) {
        this.soundHeard = soundHeard;
    }

    public String getDamageObserved() {
        return damageObserved;
    }

    public void setDamageObserved(String damageObserved) {
        this.damageObserved = damageObserved;
    }

    public Integer getBuildingFloor() {
        return buildingFloor;
    }

    public void setBuildingFloor(Integer buildingFloor) {
        this.buildingFloor = buildingFloor;
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

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Integer getCalculatedIntensity() {
        return calculatedIntensity;
    }

    public void setCalculatedIntensity(Integer calculatedIntensity) {
        this.calculatedIntensity = calculatedIntensity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
