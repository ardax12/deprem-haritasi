package com.ag.deprem.model.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_report_revision", uniqueConstraints = {
    @UniqueConstraint(name = "uq_report_revision", columnNames = {"source_report_id", "revision_number"})
})
public class SourceReportRevision {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_report_id", nullable = false)
    private SourceReport sourceReport;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "depth_km")
    private Double depthKm;

    @Column(name = "magnitude_value")
    private Double magnitudeValue;

    @Column(name = "magnitude_type", length = 16)
    private String magnitudeType;

    @Column(name = "location_text", length = 512)
    private String locationText;

    @Column(name = "country", length = 128)
    private String country;

    @Column(name = "province", length = 128)
    private String province;

    @Column(name = "district", length = 128)
    private String district;

    @Column(name = "neighborhood", length = 128)
    private String neighborhood;

    @Column(name = "source_payload_hash", nullable = false, length = 64)
    private String sourcePayloadHash;

    @Column(name = "normalized_hash", nullable = false, length = 64)
    private String normalizedHash;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "raw_payload_ref", length = 512)
    private String rawPayloadRef;

    public SourceReportRevision() {
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

    public Integer getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(Integer revisionNumber) {
        this.revisionNumber = revisionNumber;
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

    public Double getDepthKm() {
        return depthKm;
    }

    public void setDepthKm(Double depthKm) {
        this.depthKm = depthKm;
    }

    public Double getMagnitudeValue() {
        return magnitudeValue;
    }

    public void setMagnitudeValue(Double magnitudeValue) {
        this.magnitudeValue = magnitudeValue;
    }

    public String getMagnitudeType() {
        return magnitudeType;
    }

    public void setMagnitudeType(String magnitudeType) {
        this.magnitudeType = magnitudeType;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getSourcePayloadHash() {
        return sourcePayloadHash;
    }

    public void setSourcePayloadHash(String sourcePayloadHash) {
        this.sourcePayloadHash = sourcePayloadHash;
    }

    public String getNormalizedHash() {
        return normalizedHash;
    }

    public void setNormalizedHash(String normalizedHash) {
        this.normalizedHash = normalizedHash;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public String getRawPayloadRef() {
        return rawPayloadRef;
    }

    public void setRawPayloadRef(String rawPayloadRef) {
        this.rawPayloadRef = rawPayloadRef;
    }
}
