package com.ag.deprem.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AfadRawEventDto {

    @JsonProperty("eventID")
    private String eventId;

    @JsonProperty("location")
    private String location;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("depth")
    private String depth;

    @JsonProperty("type")
    private String type;

    @JsonProperty("magnitude")
    private String magnitude;

    @JsonProperty("country")
    private String country;

    @JsonProperty("province")
    private String province;

    @JsonProperty("district")
    private String district;

    @JsonProperty("neighborhood")
    private String neighborhood;

    @JsonProperty("date")
    private String date;

    @JsonProperty("isEventUpdate")
    private Boolean isEventUpdate;

    @JsonProperty("lastUpdateDate")
    private String lastUpdateDate;

    @JsonProperty("rms")
    private String rms;

    public AfadRawEventDto() {
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(String magnitude) {
        this.magnitude = magnitude;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Boolean getIsEventUpdate() {
        return isEventUpdate;
    }

    public void setIsEventUpdate(Boolean isEventUpdate) {
        this.isEventUpdate = isEventUpdate;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getRms() {
        return rms;
    }

    public void setRms(String rms) {
        this.rms = rms;
    }
}
