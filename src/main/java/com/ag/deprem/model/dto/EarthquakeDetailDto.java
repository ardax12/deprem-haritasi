package com.ag.deprem.model.dto;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeDetailDto {
    private EarthquakeItemDto current;
    private List<EarthquakeItemDto> history = new ArrayList<>();

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
}
