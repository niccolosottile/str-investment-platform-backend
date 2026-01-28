package com.str.platform.scraping.application.scheduler;

import com.str.platform.scraping.application.service.ScrapingOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for scraping job monitoring and maintenance tasks.
 * 
 * Features:
 * - Monitors for timed-out jobs and marks them as failed
 * - Can be extended for periodic data refresh
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "scraping.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class ScrapingJobScheduler {
    
    private final ScrapingOrchestrationService orchestrationService;
    
    private static final int TIMEOUT_MINUTES = 30;
    
    /**
     * Check for timed-out jobs every 10 minutes.
     * Jobs that have been in progress for more than TIMEOUT_MINUTES are marked as failed.
     */
    @Scheduled(fixedDelay = 600_000) // 10 minutes
    public void checkForTimedOutJobs() {
        log.debug("Running timeout check for scraping jobs");
        
        try {
            int timedOutCount = orchestrationService.handleTimedOutJobs(TIMEOUT_MINUTES);
            
            if (timedOutCount > 0) {
                log.warn("Marked {} scraping jobs as timed out", timedOutCount);
            } else {
                log.debug("No timed-out jobs found");
            }
            
        } catch (Exception e) {
            log.error("Error while checking for timed-out jobs", e);
        }
    }
}
