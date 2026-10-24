package com.college.lab.reliability.dto;

import com.college.lab.reliability.entity.EventRecord;
import com.college.lab.reliability.entity.EventStatus;
import com.college.lab.reliability.entity.FailureMode;

import java.time.LocalDateTime;

public class EventResponse {
    private Long id;
    private String eventId;
    private String eventType;
    private String payload;
    private FailureMode failureMode;
    private EventStatus status;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EventResponse() {
    }

    public static EventResponse fromEntity(EventRecord record) {
        EventResponse dto = new EventResponse();
        dto.setId(record.getId());
        dto.setEventId(record.getEventId());
        dto.setEventType(record.getEventType());
        dto.setPayload(record.getPayload());
        dto.setFailureMode(record.getFailureMode());
        dto.setStatus(record.getStatus());
        dto.setAttemptCount(record.getAttemptCount());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
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

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
