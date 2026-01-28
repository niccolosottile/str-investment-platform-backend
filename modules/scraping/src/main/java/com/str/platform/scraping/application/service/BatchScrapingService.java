package com.str.platform.scraping.application.service;

import com.str.platform.location.infrastructure.persistence.entity.LocationEntity;
import com.str.platform.location.infrastructure.persistence.repository.JpaLocationRepository;
import com.str.platform.scraping.application.dto.BatchScrapingRequest;
import com.str.platform.scraping.application.dto.BatchScrapingStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for batch scraping operations across multiple locations.
 * Orchestrates scraping jobs for large-scale data collection with rate limiting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchScrapingService {
    
    private final ScrapingOrchestrationService orchestrationService;
    private final JpaLocationRepository locationRepository;
    
    private final AtomicReference<UUID> currentBatchId = new AtomicReference<>(null);
    private final AtomicReference<BatchScrapingStatusResponse.BatchStatus> currentStatus = 
        new AtomicReference<>(BatchScrapingStatusResponse.BatchStatus.NOT_STARTED);
    private final AtomicInteger totalLocations = new AtomicInteger(0);
    private final AtomicInteger completedLocations = new AtomicInteger(0);
    private final AtomicInteger failedLocations = new AtomicInteger(0);
    private final AtomicReference<UUID> currentLocationId = new AtomicReference<>(null);
    private final AtomicReference<Instant> batchStartTime = new AtomicReference<>(null);
    private final AtomicInteger delayMinutes = new AtomicInteger(10);
    
    /**
     * Start batch scraping operation based on strategy.
     * Runs asynchronously to avoid blocking the HTTP request.
     * 
     * @param request Batch scraping configuration
     * @return Batch ID for tracking
     */
    public UUID scheduleBatchRefresh(BatchScrapingRequest request) {
        // Check if batch is already running
        if (currentStatus.get() == BatchScrapingStatusResponse.BatchStatus.RUNNING) {
            throw new IllegalStateException("Batch scraping is already in progress. Batch ID: " + currentBatchId.get());
        }
        
        // Fetch locations based on strategy
        List<LocationEntity> locations = fetchLocationsForBatch(request);
        
        if (locations.isEmpty()) {
            log.warn("No locations found for batch strategy: {}", request.strategy());
            throw new IllegalArgumentException("No locations available for batch scraping");
        }
        
        // Initialize batch state
        UUID batchId = UUID.randomUUID();
        currentBatchId.set(batchId);
        currentStatus.set(BatchScrapingStatusResponse.BatchStatus.RUNNING);
        totalLocations.set(locations.size());
        completedLocations.set(0);
        failedLocations.set(0);
        currentLocationId.set(null);
        batchStartTime.set(Instant.now());
        delayMinutes.set(request.delayMinutes());
        
        log.info("Starting batch scraping: batchId={}, strategy={}, totalLocations={}, delayMinutes={}", 
            batchId, request.strategy(), locations.size(), request.delayMinutes());
        
        // Start async processing
        processBatchAsync(locations, request.delayMinutes());
        
        return batchId;
    }
    
    /**
     * Get current batch scraping status.
     * 
     * @return Current status or NOT_STARTED if no batch is running
     */
    public BatchScrapingStatusResponse getBatchProgress() {
        BatchScrapingStatusResponse.BatchStatus status = currentStatus.get();
        
        if (status == BatchScrapingStatusResponse.BatchStatus.NOT_STARTED) {
            return BatchScrapingStatusResponse.notStarted();
        }
        
        int total = totalLocations.get();
        int completed = completedLocations.get();
        int failed = failedLocations.get();
        Instant startTime = batchStartTime.get();
        int delay = delayMinutes.get();
        
        // Calculate progress
        double progressPercentage = total > 0 ? (completed * 100.0 / total) : 0.0;
        
        // Estimate completion time
        Instant estimatedCompletion = null;
        if (startTime != null && total > 0 && completed > 0) {
            long elapsedMinutes = Duration.between(startTime, Instant.now()).toMinutes();
            double avgMinutesPerLocation = (double) elapsedMinutes / completed;
            long remainingLocations = total - completed;
            long estimatedRemainingMinutes = (long) (remainingLocations * avgMinutesPerLocation);
            estimatedCompletion = Instant.now().plus(estimatedRemainingMinutes, ChronoUnit.MINUTES);
        }
        
        return new BatchScrapingStatusResponse(
            currentBatchId.get(),
            status,
            total,
            completed,
            failed,
            currentLocationId.get(),
            startTime,
            estimatedCompletion,
            delay,
            progressPercentage
        );
    }
    
    /**
     * Fetch locations based on batch strategy.
     */
    private List<LocationEntity> fetchLocationsForBatch(BatchScrapingRequest request) {
        return switch (request.strategy()) {
            case ALL_LOCATIONS -> {
                log.info("Fetching all locations from database");
                yield locationRepository.findAll();
            }
            case STALE_ONLY -> {
                Instant threshold = Instant.now().minus(request.staleThresholdDays(), ChronoUnit.DAYS);
                log.info("Fetching stale locations (older than {} days)", request.staleThresholdDays());
                yield locationRepository.findStaleLocations(threshold);
            }
        };
    }
    
    /**
     * Process batch asynchronously with delays between locations.
     * Runs in a separate thread to avoid blocking.
     */
    @Async
    protected void processBatchAsync(List<LocationEntity> locations, int delayMinutes) {
        log.info("Starting async batch processing for {} locations with {} minute delays", 
            locations.size(), delayMinutes);
        
        try {
            for (LocationEntity location : locations) {
                try {
                    currentLocationId.set(location.getId());
                    
                    log.info("Processing location {}/{}: {} ({})", 
                        completedLocations.get() + 1, 
                        totalLocations.get(),
                        location.getCity(), 
                        location.getId());
                    
                    // Orchestrate comprehensive scraping for location
                    orchestrationService.orchestrateLocationAnalysis(location.getId());
                    
                    completedLocations.incrementAndGet();
                    
                    // Delay before next location (except for last one)
                    if (completedLocations.get() < totalLocations.get()) {
                        log.debug("Waiting {} minutes before next location", delayMinutes);
                        Thread.sleep(delayMinutes * 60 * 1000L);
                    }
                    
                } catch (InterruptedException e) {
                    log.warn("Batch processing interrupted at location: {}", location.getId());
                    Thread.currentThread().interrupt();
                    currentStatus.set(BatchScrapingStatusResponse.BatchStatus.FAILED);
                    return;
                    
                } catch (Exception e) {
                    log.error("Failed to process location: {} ({})", location.getId(), location.getCity(), e);
                    failedLocations.incrementAndGet();
                    completedLocations.incrementAndGet();
                    // Continue with next location
                }
            }
            
            // Batch completed successfully
            currentStatus.set(BatchScrapingStatusResponse.BatchStatus.COMPLETED);
            currentLocationId.set(null);
            
            log.info("Batch scraping completed: batchId={}, total={}, completed={}, failed={}, duration={} minutes",
                currentBatchId.get(),
                totalLocations.get(),
                completedLocations.get(),
                failedLocations.get(),
                Duration.between(batchStartTime.get(), Instant.now()).toMinutes());
            
        } catch (Exception e) {
            log.error("Batch processing failed with unexpected error", e);
            currentStatus.set(BatchScrapingStatusResponse.BatchStatus.FAILED);
        }
    }
}
