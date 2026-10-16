package com.ag.deprem.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SourceStatusTracker {

    public static class SourceState {
        private final String sourceName;
        private Instant lastSuccessfulFetchAt;
        private Instant lastAttemptAt;
        private String lastError;
        private boolean coverageComplete = false;

        public SourceState(String sourceName) {
            this.sourceName = sourceName;
        }

        public synchronized void recordSuccess(Instant time) {
            this.lastSuccessfulFetchAt = time;
            this.lastAttemptAt = time;
            this.lastError = null;
        }

        public synchronized void recordAttempt(Instant time) {
            this.lastAttemptAt = time;
        }

        public synchronized void recordFailure(Instant time, String error) {
            this.lastAttemptAt = time;
            this.lastError = error;
        }

        public synchronized String getFreshnessStatus(Instant now) {
            if (lastSuccessfulFetchAt == null) {
                return lastError != null ? "error" : "delayed";
            }
            long minutesAgo = Duration.between(lastSuccessfulFetchAt, now).toMinutes();
            if (minutesAgo > 60) {
                return "stale";
            } else if (minutesAgo > 20) {
                return "delayed";
            } else {
                return "current";
            }
        }

        public String getSourceName() { return sourceName; }
        public synchronized Instant getLastSuccessfulFetchAt() { return lastSuccessfulFetchAt; }
        public synchronized Instant getLastAttemptAt() { return lastAttemptAt; }
        public synchronized String getLastError() { return lastError; }
        public synchronized boolean isCoverageComplete() { return coverageComplete; }
        public synchronized void setCoverageComplete(boolean coverageComplete) { this.coverageComplete = coverageComplete; }
    }

    private final Map<String, SourceState> states = new ConcurrentHashMap<>();

    public SourceStatusTracker() {
        states.put("AFAD", new SourceState("AFAD"));
        states.put("KOERI", new SourceState("KOERI"));
    }

    public SourceState getSourceState(String source) {
        return states.computeIfAbsent(source.toUpperCase(), SourceState::new);
    }

    public void recordSuccess(String source, Instant time) {
        getSourceState(source).recordSuccess(time);
    }

    public void recordAttempt(String source, Instant time) {
        getSourceState(source).recordAttempt(time);
    }

    public void recordFailure(String source, Instant time, String error) {
        getSourceState(source).recordFailure(time, error);
    }

    public String getFreshnessStatus(String source, Instant now) {
        return getSourceState(source).getFreshnessStatus(now);
    }

    public Instant getLastSuccessfulFetchAt(String source) {
        return getSourceState(source).getLastSuccessfulFetchAt();
    }

    // --- Backward compatibility methods (defaults to AFAD) ---

    public void recordSuccess(Instant time) {
        recordSuccess("AFAD", time);
    }

    public void recordAttempt(Instant time) {
        recordAttempt("AFAD", time);
    }

    public void recordFailure(Instant time, String error) {
        recordFailure("AFAD", time, error);
    }

    public String getFreshnessStatus(Instant now) {
        return getFreshnessStatus("AFAD", now);
    }

    public Instant getLastSuccessfulFetchAt() {
        return getSourceState("AFAD").getLastSuccessfulFetchAt();
    }

    public Instant getLastAttemptAt() {
        return getSourceState("AFAD").getLastAttemptAt();
    }

    public String getLastError() {
        return getSourceState("AFAD").getLastError();
    }

    public boolean isCoverageComplete() {
        return getSourceState("AFAD").isCoverageComplete();
    }

    public void setCoverageComplete(boolean coverageComplete) {
        getSourceState("AFAD").setCoverageComplete(coverageComplete);
    }
}
