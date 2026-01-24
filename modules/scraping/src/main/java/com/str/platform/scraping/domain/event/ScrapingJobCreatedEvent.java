package com.str.platform.scraping.domain.event;

import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.shared.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a scraping job is created.
 * Python workers listen to this to start scraping.
 */
@Getter
@Builder
@AllArgsConstructor
public class ScrapingJobCreatedEvent implements DomainEvent {
    
    private final UUID jobId;
    private final UUID locationId;
    private final String locationName;
    
    /**
     * Type of scraping job to execute
     */
    @Builder.Default
    private final JobType jobType = JobType.FULL_PROFILE;
    
    /**
     * Platform to scrape
     */
    private final ScrapingJob.Platform platform;
    
    /**
     * Bounding box for property filtering (optional, replaces lat/lng/radius)
     */
    @Nullable
    private final Double boundingBoxSwLng;
    @Nullable
    private final Double boundingBoxSwLat;
    @Nullable
    private final Double boundingBoxNeLng;
    @Nullable
    private final Double boundingBoxNeLat;
    
    /**
     * Check-in date for price search
     */
    private final LocalDate searchDateStart;
    
    /**
     * Check-out date for price search
     */
    private final LocalDate searchDateEnd;
    
    private final LocalDateTime occurredAt;
    
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
