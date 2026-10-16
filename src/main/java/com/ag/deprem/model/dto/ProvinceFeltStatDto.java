package com.ag.deprem.model.dto;

public class ProvinceFeltStatDto {
    private String province;
    private long count;
    private double avgIntensity;

    public ProvinceFeltStatDto() {
    }

    public ProvinceFeltStatDto(String province, long count, double avgIntensity) {
        this.province = province;
        this.count = count;
        this.avgIntensity = avgIntensity;
    }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public double getAvgIntensity() { return avgIntensity; }
    public void setAvgIntensity(double avgIntensity) { this.avgIntensity = avgIntensity; }
}
