package com.ag.deprem.model.dto;

import java.util.ArrayList;
import java.util.List;

public class FeltReportSummaryDto {
    private long totalReports;
    private double averageIntensity;
    private String intensityCategory;
    private String dominantReaction;
    private List<ProvinceFeltStatDto> reportsByProvince = new ArrayList<>();
    private boolean alreadyReportedByClient;

    public FeltReportSummaryDto() {
    }

    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }

    public double getAverageIntensity() { return averageIntensity; }
    public void setAverageIntensity(double averageIntensity) { this.averageIntensity = averageIntensity; }

    public String getIntensityCategory() { return intensityCategory; }
    public void setIntensityCategory(String intensityCategory) { this.intensityCategory = intensityCategory; }

    public String getDominantReaction() { return dominantReaction; }
    public void setDominantReaction(String dominantReaction) { this.dominantReaction = dominantReaction; }

    public List<ProvinceFeltStatDto> getReportsByProvince() { return reportsByProvince; }
    public void setReportsByProvince(List<ProvinceFeltStatDto> reportsByProvince) { this.reportsByProvince = reportsByProvince; }

    public boolean isAlreadyReportedByClient() { return alreadyReportedByClient; }
    public void setAlreadyReportedByClient(boolean alreadyReportedByClient) { this.alreadyReportedByClient = alreadyReportedByClient; }
}
