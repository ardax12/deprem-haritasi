package com.ag.deprem.model.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class NormalizedReportDto {
    private String source;
    private String sourceEventId;
    private Instant occurredAt;
    private Double latitude;
    private Double longitude;
    private Double depthKm;
    private Double magnitudeValue;
    private String magnitudeType;
    private String locationText;
    private String country;
    private String province;
    private String district;
    private String neighborhood;
    private Boolean isEventUpdate;
    private Instant sourceUpdatedAt;
    private String sourcePayloadHash;
    private String normalizedHash;
    private boolean valid;
    private List<String> dataQualityNotes = new ArrayList<>();

    public NormalizedReportDto() {
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

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getDataQualityNotes() {
        return dataQualityNotes;
    }

    public void setDataQualityNotes(List<String> dataQualityNotes) {
        this.dataQualityNotes = dataQualityNotes;
    }
}
