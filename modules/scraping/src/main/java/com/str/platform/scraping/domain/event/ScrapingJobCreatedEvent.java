package com.str.platform.scraping.domain.event;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.shared.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a scraping job is created.
 * Python workers listen to this to start scraping.
 */
@Getter
@AllArgsConstructor
public class ScrapingJobCreatedEvent implements DomainEvent {
    
    private final UUID jobId;
    private final UUID locationId;
    private final double latitude;
    private final double longitude;
    private final ScrapingJob.Platform platform;
    private final int radiusKm;
    private final LocalDateTime occurredAt;
    
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
