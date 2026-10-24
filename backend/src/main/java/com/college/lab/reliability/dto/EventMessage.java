package com.college.lab.reliability.dto;

import com.college.lab.reliability.entity.FailureMode;
import java.time.LocalDateTime;

public class EventMessage {
    private String eventId;
    private String eventType;
    private String payload;
    private FailureMode failureMode;
    private LocalDateTime timestamp;

    public EventMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public EventMessage(String eventId, String eventType, String payload, FailureMode failureMode) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.failureMode = failureMode;
        this.timestamp = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public FailureMode getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
