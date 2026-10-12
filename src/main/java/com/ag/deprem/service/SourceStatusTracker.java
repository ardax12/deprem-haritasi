package com.ag.deprem.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class SourceStatusTracker {

    private Instant lastSuccessfulFetchAt;
    private Instant lastAttemptAt;
    private String lastError;
    private boolean coverageComplete = false;

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
            return "delayed";
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

    public synchronized Instant getLastSuccessfulFetchAt() {
        return lastSuccessfulFetchAt;
    }

    public synchronized Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public synchronized String getLastError() {
        return lastError;
    }

    public synchronized boolean isCoverageComplete() {
        return coverageComplete;
    }

    public synchronized void setCoverageComplete(boolean coverageComplete) {
        this.coverageComplete = coverageComplete;
    }
}
