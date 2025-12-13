package com.str.platform.scraping.domain.event;

import com.str.platform.shared.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a scraping job fails.
 */
@Getter
@AllArgsConstructor
public class ScrapingJobFailedEvent implements DomainEvent {
    
    private final UUID jobId;
    private final String errorMessage;
    private final String errorType;
    private final LocalDateTime occurredAt;
    
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
