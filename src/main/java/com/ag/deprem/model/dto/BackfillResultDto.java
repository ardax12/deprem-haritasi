package com.ag.deprem.model.dto;

import java.time.Instant;

public class BackfillResultDto {
    private Instant startAt;
    private Instant endAt;
    private int totalEventsFetched;
    private int inserted;
    private int updated;
    private int unchanged;
    private int rejected;
    private int chunksProcessed;
    private boolean success;
    private String message;

    public BackfillResultDto() {
    }

    public BackfillResultDto(Instant startAt,
                             Instant endAt,
                             int totalEventsFetched,
                             int inserted,
                             int updated,
                             int unchanged,
                             int rejected,
                             int chunksProcessed,
                             boolean success,
                             String message) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.totalEventsFetched = totalEventsFetched;
        this.inserted = inserted;
        this.updated = updated;
        this.unchanged = unchanged;
        this.rejected = rejected;
        this.chunksProcessed = chunksProcessed;
        this.success = success;
        this.message = message;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public int getTotalEventsFetched() {
        return totalEventsFetched;
    }

    public void setTotalEventsFetched(int totalEventsFetched) {
        this.totalEventsFetched = totalEventsFetched;
    }

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public void setUnchanged(int unchanged) {
        this.unchanged = unchanged;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public int getChunksProcessed() {
        return chunksProcessed;
    }

    public void setChunksProcessed(int chunksProcessed) {
        this.chunksProcessed = chunksProcessed;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
