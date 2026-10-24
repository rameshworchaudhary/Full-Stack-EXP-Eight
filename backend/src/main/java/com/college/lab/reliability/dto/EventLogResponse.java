package com.college.lab.reliability.dto;

import com.college.lab.reliability.entity.EventLog;
import java.time.LocalDateTime;

public class EventLogResponse {
    private Long id;
    private String eventId;
    private int attemptNumber;
    private String status;
    private long backoffDelayMs;
    private String message;
    private LocalDateTime timestamp;

    public EventLogResponse() {
    }

    public static EventLogResponse fromEntity(EventLog log) {
        EventLogResponse dto = new EventLogResponse();
        dto.setId(log.getId());
        dto.setEventId(log.getEventId());
        dto.setAttemptNumber(log.getAttemptNumber());
        dto.setStatus(log.getStatus());
        dto.setBackoffDelayMs(log.getBackoffDelayMs());
        dto.setMessage(log.getMessage());
        dto.setTimestamp(log.getTimestamp());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getBackoffDelayMs() {
        return backoffDelayMs;
    }

    public void setBackoffDelayMs(long backoffDelayMs) {
        this.backoffDelayMs = backoffDelayMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
