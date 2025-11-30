package com.str.platform.scraping.domain.model;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Scraping Job aggregate root.
 * Represents a job to scrape STR data for a location.
 */
@Getter
public class ScrapingJob extends BaseEntity {
    
    private Coordinates location;
    private Platform platform;
    private int radiusKm;
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
    
    public ScrapingJob(Coordinates location, Platform platform, int radiusKm) {
        super();
        this.location = location;
        this.platform = platform;
        this.radiusKm = radiusKm;
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
