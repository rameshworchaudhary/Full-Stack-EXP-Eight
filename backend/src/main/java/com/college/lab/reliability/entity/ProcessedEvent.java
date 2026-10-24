package com.college.lab.reliability.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_processed_event_id", columnList = "event_id", unique = true)
})
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "result_summary", length = 255)
    private String resultSummary;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String resultSummary) {
        this.eventId = eventId;
        this.resultSummary = resultSummary;
        this.processedAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }
}
