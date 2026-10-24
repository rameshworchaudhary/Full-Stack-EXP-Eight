package com.college.lab.reliability.entity;

public enum EventStatus {
    SUBMITTED,
    PROCESSING,
    RETRYING,
    SUCCESS,
    FAILED,
    DUPLICATE_IGNORED,
    DLQ
}
