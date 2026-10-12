package com.ag.deprem.model.dto;

import java.time.Instant;

public class FreshnessDto {
    private String source;
    private Instant lastSuccessfulFetchAt;
    private String status;

    public FreshnessDto() {
    }

    public FreshnessDto(String source, Instant lastSuccessfulFetchAt, String status) {
        this.source = source;
        this.lastSuccessfulFetchAt = lastSuccessfulFetchAt;
        this.status = status;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getLastSuccessfulFetchAt() { return lastSuccessfulFetchAt; }
    public void setLastSuccessfulFetchAt(Instant lastSuccessfulFetchAt) { this.lastSuccessfulFetchAt = lastSuccessfulFetchAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
