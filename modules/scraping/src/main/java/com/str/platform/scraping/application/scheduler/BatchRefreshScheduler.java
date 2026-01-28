package com.str.platform.scraping.application.scheduler;

import com.str.platform.scraping.application.dto.BatchScrapingRequest;
import com.str.platform.scraping.application.service.BatchScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for automated batch scraping refresh.
 * 
 * Runs monthly to refresh stale location data automatically.
 * Can be enabled/disabled via configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "batch.scraping.auto-schedule.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class BatchRefreshScheduler {
    
    private final BatchScrapingService batchScrapingService;
    
    @Value("${batch.scraping.delay-minutes:10}")
    private int delayMinutes;
    
    @Value("${batch.scraping.stale-threshold-days:30}")
    private int staleThresholdDays;
    
    /**
     * Run batch refresh on the 1st day of every month at 1 AM.
     * Targets only stale locations to avoid unnecessary load.
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void scheduledBatchRefresh() {
        log.info("Starting scheduled monthly batch refresh (delayMinutes={}, staleThresholdDays={})",
            delayMinutes, staleThresholdDays);
        
        try {
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.STALE_ONLY,
                delayMinutes,
                staleThresholdDays
            );
            
            batchScrapingService.scheduleBatchRefresh(request);
            log.info("Scheduled batch refresh initiated successfully");
            
        } catch (Exception e) {
            log.error("Failed to start scheduled batch refresh", e);
        }
    }
}
