package com.str.platform.scraping.domain.model;

import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Scraping Job aggregate root.
 * Represents a job to scrape STR data for a location.
 */
@Getter
public class ScrapingJob extends BaseEntity {
    
    private UUID locationId;
    private Platform platform;
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer propertiesFound;
    private String errorMessage;
    
    public enum Platform {
        AIRBNB,
        BOOKING,
        VRBO
    }
    
    public enum JobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    
    protected ScrapingJob() {
        super();
    }
    
    public ScrapingJob(UUID locationId, Platform platform) {
        super();
        if (locationId == null) {
            throw new IllegalArgumentException("Location ID cannot be null");
        }
        if (platform == null) {
            throw new IllegalArgumentException("Platform cannot be null");
        }
        this.locationId = locationId;
        this.platform = platform;
        this.status = JobStatus.PENDING;
    }
    
    public void start() {
        if (status != JobStatus.PENDING) {
            throw new IllegalStateException("Job already started");
        }
        this.status = JobStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        markAsUpdated();
    }
    
    public void complete(int propertiesFound) {
        if (status != JobStatus.IN_PROGRESS) {
            throw new IllegalStateException("Job not in progress");
        }
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.propertiesFound = propertiesFound;
        markAsUpdated();
    }
    
    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        markAsUpdated();
    }
    
    public Duration getExecutionTime() {
        if (startedAt == null || completedAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, completedAt);
    }
}
