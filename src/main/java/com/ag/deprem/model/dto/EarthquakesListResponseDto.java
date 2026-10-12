package com.ag.deprem.model.dto;

import java.util.ArrayList;
import java.util.List;

public class EarthquakesListResponseDto {
    private List<EarthquakeItemDto> items = new ArrayList<>();
    private PageInfoDto page;
    private FreshnessDto freshness;

    public EarthquakesListResponseDto() {
    }

    public EarthquakesListResponseDto(List<EarthquakeItemDto> items, PageInfoDto page, FreshnessDto freshness) {
        this.items = items;
        this.page = page;
        this.freshness = freshness;
    }

    public List<EarthquakeItemDto> getItems() { return items; }
    public void setItems(List<EarthquakeItemDto> items) { this.items = items; }

    public PageInfoDto getPage() { return page; }
    public void setPage(PageInfoDto page) { this.page = page; }

    public FreshnessDto getFreshness() { return freshness; }
    public void setFreshness(FreshnessDto freshness) { this.freshness = freshness; }
}
