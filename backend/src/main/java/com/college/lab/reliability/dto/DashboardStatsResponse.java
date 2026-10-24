package com.college.lab.reliability.dto;

public class DashboardStatsResponse {
    private long totalSubmitted;
    private long successfullyProcessed;
    private long failed;
    private long retrying;
    private long duplicateIgnored;
    private long dlqEvents;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalSubmitted, long successfullyProcessed, long failed,
                                  long retrying, long duplicateIgnored, long dlqEvents) {
        this.totalSubmitted = totalSubmitted;
        this.successfullyProcessed = successfullyProcessed;
        this.failed = failed;
        this.retrying = retrying;
        this.duplicateIgnored = duplicateIgnored;
        this.dlqEvents = dlqEvents;
    }

    public long getTotalSubmitted() {
        return totalSubmitted;
    }

    public void setTotalSubmitted(long totalSubmitted) {
        this.totalSubmitted = totalSubmitted;
    }

    public long getSuccessfullyProcessed() {
        return successfullyProcessed;
    }

    public void setSuccessfullyProcessed(long successfullyProcessed) {
        this.successfullyProcessed = successfullyProcessed;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getRetrying() {
        return retrying;
    }

    public void setRetrying(long retrying) {
        this.retrying = retrying;
    }

    public long getDuplicateIgnored() {
        return duplicateIgnored;
    }

    public void setDuplicateIgnored(long duplicateIgnored) {
        this.duplicateIgnored = duplicateIgnored;
    }

    public long getDlqEvents() {
        return dlqEvents;
    }

    public void setDlqEvents(long dlqEvents) {
        this.dlqEvents = dlqEvents;
    }
}
