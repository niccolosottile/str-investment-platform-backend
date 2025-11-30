package com.str.platform.scraping.domain.event;

import com.str.platform.shared.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a scraping job completes successfully.
 */
@Getter
@AllArgsConstructor
public class ScrapingJobCompletedEvent implements DomainEvent {
    
    private final UUID jobId;
    private final int propertiesFound;
    private final Duration executionTime;
    private final LocalDateTime occurredAt;
    
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
