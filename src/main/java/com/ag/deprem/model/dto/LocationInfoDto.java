package com.ag.deprem.model.dto;

public class LocationInfoDto {
    private Boolean isOffshore;
    private Double distanceToCoastKm;
    private String nearestSettlement;
    private String environment;
    private String displayLocation;

    public LocationInfoDto() {
    }

    public LocationInfoDto(Boolean isOffshore, Double distanceToCoastKm, String nearestSettlement, String environment, String displayLocation) {
        this.isOffshore = isOffshore;
        this.distanceToCoastKm = distanceToCoastKm;
        this.nearestSettlement = nearestSettlement;
        this.environment = environment;
        this.displayLocation = displayLocation;
    }

    public Boolean getIsOffshore() {
        return isOffshore;
    }

    public void setIsOffshore(Boolean isOffshore) {
        this.isOffshore = isOffshore;
    }

    public Double getDistanceToCoastKm() {
        return distanceToCoastKm;
    }

    public void setDistanceToCoastKm(Double distanceToCoastKm) {
        this.distanceToCoastKm = distanceToCoastKm;
    }

    public String getNearestSettlement() {
        return nearestSettlement;
    }

    public void setNearestSettlement(String nearestSettlement) {
        this.nearestSettlement = nearestSettlement;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDisplayLocation() {
        return displayLocation;
    }

    public void setDisplayLocation(String displayLocation) {
        this.displayLocation = displayLocation;
    }
}
