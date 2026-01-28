package com.str.platform.analysis.infrastructure.event;

import com.str.platform.analysis.application.service.AnalysisOrchestrationService;
import com.str.platform.shared.event.ScrapingDataUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for scraping data update events and triggers cache eviction.
 * Decouples scraping module from analysis module to avoid circular dependencies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingDataEventListener {
    
    private final AnalysisOrchestrationService analysisOrchestrationService;
    
    /**
     * Handle scraping data updated event by evicting cached analysis results.
     * Async to avoid blocking scraping result processing.
     */
    @Async
    @EventListener
    public void handleScrapingDataUpdated(ScrapingDataUpdatedEvent event) {
        log.debug("Received scraping data update for locationId={}, properties={}", 
            event.locationId(), event.propertiesCount());
        
        try {
            analysisOrchestrationService.evictAnalysisCacheForLocation(event.locationId());
        } catch (Exception e) {
            log.error("Failed to evict analysis cache for locationId={}", event.locationId(), e);
            // Non-critical - don't propagate
        }
    }
}