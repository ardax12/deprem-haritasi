package com.ag.deprem.model.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StatusResponseDto {
    private String application = "available";
    private String mode = "live";
    private List<SourceStatusDto> sources = new ArrayList<>();

    public static class SourceStatusDto {
        private String name;
        private String status;
        private Instant lastSuccessfulFetchAt;
        private Instant lastAttemptAt;
        private CoverageDto coverage;

        public SourceStatusDto() {
        }

        public SourceStatusDto(String name, String status, Instant lastSuccessfulFetchAt, Instant lastAttemptAt, CoverageDto coverage) {
            this.name = name;
            this.status = status;
            this.lastSuccessfulFetchAt = lastSuccessfulFetchAt;
            this.lastAttemptAt = lastAttemptAt;
            this.coverage = coverage;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Instant getLastSuccessfulFetchAt() { return lastSuccessfulFetchAt; }
        public void setLastSuccessfulFetchAt(Instant lastSuccessfulFetchAt) { this.lastSuccessfulFetchAt = lastSuccessfulFetchAt; }

        public Instant getLastAttemptAt() { return lastAttemptAt; }
        public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

        public CoverageDto getCoverage() { return coverage; }
        public void setCoverage(CoverageDto coverage) { this.coverage = coverage; }
    }

    public static class CoverageDto {
        private Instant from;
        private Instant to;
        private boolean complete;

        public CoverageDto() {
        }

        public CoverageDto(Instant from, Instant to, boolean complete) {
            this.from = from;
            this.to = to;
            this.complete = complete;
        }

        public Instant getFrom() { return from; }
        public void setFrom(Instant from) { this.from = from; }

        public Instant getTo() { return to; }
        public void setTo(Instant to) { this.to = to; }

        public boolean isComplete() { return complete; }
        public void setComplete(boolean complete) { this.complete = complete; }
    }

    public StatusResponseDto() {
    }

    public StatusResponseDto(String application, String mode, List<SourceStatusDto> sources) {
        this.application = application;
        this.mode = mode;
        this.sources = sources;
    }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<SourceStatusDto> getSources() { return sources; }
    public void setSources(List<SourceStatusDto> sources) { this.sources = sources; }
}
