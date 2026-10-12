package com.ag.deprem.model.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EarthquakeItemDto {
    private String id;
    private Instant occurredAt;
    private Instant retrievedAt;
    private Double latitude;
    private Double longitude;
    private Double depthKm;
    private MagnitudeDto magnitude;
    private String place;
    private String country;
    private String province;
    private String district;
    private String neighborhood;
    private String source;
    private String revisionStatus;
    private Integer revisionNumber;
    private List<String> dataQuality = new ArrayList<>();
    private Boolean isOffshore;
    private Double distanceToCoastKm;
    private String nearestSettlement;
    private String environment;
    private String displayLocation;

    public EarthquakeItemDto() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Instant getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDepthKm() { return depthKm; }
    public void setDepthKm(Double depthKm) { this.depthKm = depthKm; }

    public MagnitudeDto getMagnitude() { return magnitude; }
    public void setMagnitude(MagnitudeDto magnitude) { this.magnitude = magnitude; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getRevisionStatus() { return revisionStatus; }
    public void setRevisionStatus(String revisionStatus) { this.revisionStatus = revisionStatus; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

    public List<String> getDataQuality() { return dataQuality; }
    public void setDataQuality(List<String> dataQuality) { this.dataQuality = dataQuality; }

    public Boolean getIsOffshore() { return isOffshore; }
    public void setIsOffshore(Boolean isOffshore) { this.isOffshore = isOffshore; }

    public Double getDistanceToCoastKm() { return distanceToCoastKm; }
    public void setDistanceToCoastKm(Double distanceToCoastKm) { this.distanceToCoastKm = distanceToCoastKm; }

    public String getNearestSettlement() { return nearestSettlement; }
    public void setNearestSettlement(String nearestSettlement) { this.nearestSettlement = nearestSettlement; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getDisplayLocation() { return displayLocation; }
    public void setDisplayLocation(String displayLocation) { this.displayLocation = displayLocation; }
}
