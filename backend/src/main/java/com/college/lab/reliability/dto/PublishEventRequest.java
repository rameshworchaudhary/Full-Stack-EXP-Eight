package com.college.lab.reliability.dto;

import com.college.lab.reliability.entity.FailureMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PublishEventRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "payload is required")
    private String payload;

    @NotNull(message = "failureMode is required (NONE, TRANSIENT, PERMANENT)")
    private FailureMode failureMode;

    public PublishEventRequest() {
    }

    public PublishEventRequest(String eventId, String eventType, String payload, FailureMode failureMode) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.failureMode = failureMode;
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
}
