package com.ag.deprem.model.dto;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeDetailDto {
    private EarthquakeItemDto current;
    private List<EarthquakeItemDto> history = new ArrayList<>();
    private EarthquakeItemDto matchedPartner;
    private String matchStatus;
    private String matchReason;
    private Double matchScore;

    public EarthquakeDetailDto() {
    }

    public EarthquakeDetailDto(EarthquakeItemDto current, List<EarthquakeItemDto> history) {
        this.current = current;
        this.history = history;
    }

    public EarthquakeItemDto getCurrent() { return current; }
    public void setCurrent(EarthquakeItemDto current) { this.current = current; }

    public List<EarthquakeItemDto> getHistory() { return history; }
    public void setHistory(List<EarthquakeItemDto> history) { this.history = history; }

    public EarthquakeItemDto getMatchedPartner() { return matchedPartner; }
    public void setMatchedPartner(EarthquakeItemDto matchedPartner) { this.matchedPartner = matchedPartner; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
}
