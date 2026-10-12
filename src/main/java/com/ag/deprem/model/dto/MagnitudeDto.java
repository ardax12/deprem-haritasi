package com.ag.deprem.model.dto;

public class MagnitudeDto {
    private Double value;
    private String type;

    public MagnitudeDto() {
    }

    public MagnitudeDto(Double value, String type) {
        this.value = value;
        this.type = type;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
