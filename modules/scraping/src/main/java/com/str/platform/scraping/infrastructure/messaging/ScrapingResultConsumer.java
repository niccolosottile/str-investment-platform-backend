package com.str.platform.scraping.infrastructure.messaging;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.Location;
import com.str.platform.scraping.domain.event.ScrapingJobCompletedEvent;
import com.str.platform.scraping.domain.event.ScrapingJobFailedEvent;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumer for scraping result events from Python workers.
 * Processes completed and failed scraping jobs.
 * 
 * Note: Queue name is defined here to avoid circular dependency with application module.
 * Must match the configuration in RabbitMQConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingResultConsumer {
    
    private final JpaScrapingJobRepository scrapingJobRepository;
    private final JpaPropertyRepository propertyRepository;
    private final CacheManager cacheManager;
    private final LocationService locationService;
    
    private static final String SCRAPING_RESULT_QUEUE = "str.scraping.result.queue";
    
    /**
     * Handle scraping job completed event from Python worker.
     * Updates job status and saves scraped properties.
     */
    @RabbitListener(queues = SCRAPING_RESULT_QUEUE)
    @Transactional
    public void handleJobCompleted(ScrapingJobCompletedEvent event) {
        log.info("Received scraping job completed event: jobId={}, propertiesFound={}",
            event.getJobId(), event.getPropertiesFound());
        
        try {
            // Find and update job
            ScrapingJobEntity jobEntity = scrapingJobRepository.findById(event.getJobId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Scraping job not found: " + event.getJobId()));
            
            jobEntity.setStatus(ScrapingJobEntity.JobStatus.COMPLETED);
            jobEntity.setCompletedAt(event.getOccurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            jobEntity.setPropertiesFound(event.getPropertiesFound());
            scrapingJobRepository.save(jobEntity);
            
            // Find or create Location for this scraping job
            Location location = locationService.findOrCreateByCoordinates(
                jobEntity.getLatitude().doubleValue(),
                jobEntity.getLongitude().doubleValue()
            );
            UUID locationId = location.getId();
            
            // Save or update properties
            int savedCount = 0;
            for (ScrapingJobCompletedEvent.PropertyData propData : event.getProperties()) {
                try {
                    // Check if property already exists
                    PropertyEntity.Platform platform = PropertyEntity.Platform.valueOf(propData.getPlatform());
                    PropertyEntity existingProperty = propertyRepository
                        .findByPlatformAndPlatformPropertyId(platform, propData.getPlatformId())
                        .orElse(null);
                    
                    if (existingProperty != null) {
                        // Update existing property
                        updatePropertyEntity(existingProperty, propData, locationId);
                        propertyRepository.save(existingProperty);
                        log.debug("Updated existing property: {}", propData.getPlatformId());
                    } else {
                        // Create new property
                        PropertyEntity newProperty = createPropertyEntity(propData, locationId);
                        propertyRepository.save(newProperty);
                        log.debug("Created new property: {}", propData.getPlatformId());
                    }
                    savedCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to save property: {}", propData.getPlatformId(), e);
                    // Continue processing other properties
                }
            }
            
            log.info("Saved {} properties for job: {}", savedCount, event.getJobId());
            
            // Invalidate related caches
            invalidateCaches(jobEntity.getLatitude().doubleValue(), jobEntity.getLongitude().doubleValue());
            
        } catch (Exception e) {
            log.error("Failed to process scraping job completed event: jobId={}", 
                event.getJobId(), e);
            throw e; // Will trigger retry or DLQ
        }
    }
    
    /**
     * Handle scraping job failed event from Python worker.
     * Updates job status with error information.
     */
    @RabbitListener(queues = SCRAPING_RESULT_QUEUE)
    @Transactional
    public void handleJobFailed(ScrapingJobFailedEvent event) {
        log.warn("Received scraping job failed event: jobId={}, error={}",
            event.getJobId(), event.getErrorMessage());
        
        try {
            ScrapingJobEntity jobEntity = scrapingJobRepository.findById(event.getJobId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Scraping job not found: " + event.getJobId()));
            
            jobEntity.setStatus(ScrapingJobEntity.JobStatus.FAILED);
            jobEntity.setCompletedAt(event.getOccurredAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            jobEntity.setErrorMessage(event.getErrorMessage());
            scrapingJobRepository.save(jobEntity);
            
            log.info("Marked scraping job as failed: jobId={}", event.getJobId());
            
        } catch (Exception e) {
            log.error("Failed to process scraping job failed event: jobId={}", 
                event.getJobId(), e);
            throw e;
        }
    }
    
    /**
     * Create new property entity from event data
     */
    private PropertyEntity createPropertyEntity(ScrapingJobCompletedEvent.PropertyData propData, 
                                                UUID locationId) {
        return PropertyEntity.builder()
            .locationId(locationId)
            .platform(PropertyEntity.Platform.valueOf(propData.getPlatform()))
            .platformPropertyId(propData.getPlatformId())
            .latitude(java.math.BigDecimal.valueOf(propData.getLatitude()))
            .longitude(java.math.BigDecimal.valueOf(propData.getLongitude()))
            .title(propData.getTitle())
            .propertyType(propData.getPropertyType())
            .price(propData.getPrice())
            .currency(propData.getCurrency())
            .bedrooms(propData.getBedrooms())
            .bathrooms(propData.getBathrooms())
            .guests(propData.getGuests())
            .rating(propData.getRating())
            .reviewCount(propData.getReviewCount())
            .isSuperhost(propData.getIsSuperhost())
            .imageUrl(propData.getImageUrl())
            .propertyUrl(propData.getPropertyUrl())
            .build();
    }
    
    /**
     * Update existing property entity with new data
     */
    private void updatePropertyEntity(PropertyEntity entity, ScrapingJobCompletedEvent.PropertyData propData,
                                     UUID locationId) {
        entity.setLocationId(locationId);
        entity.setTitle(propData.getTitle());
        entity.setPropertyType(propData.getPropertyType());
        entity.setPrice(propData.getPrice());
        entity.setCurrency(propData.getCurrency());
        entity.setBedrooms(propData.getBedrooms());
        entity.setBathrooms(propData.getBathrooms());
        entity.setGuests(propData.getGuests());
        entity.setRating(propData.getRating());
        entity.setReviewCount(propData.getReviewCount());
        entity.setIsSuperhost(propData.getIsSuperhost());
        entity.setImageUrl(propData.getImageUrl());
        entity.setPropertyUrl(propData.getPropertyUrl());
    }
    
    /**
     * Invalidate caches related to the scraped location
     */
    private void invalidateCaches(double lat, double lng) {
        try {
            // Invalidate analysis results cache for this location
            var analysisCache = cacheManager.getCache("analysisResults");
            if (analysisCache != null) {
                analysisCache.clear();
                log.debug("Cleared analysisResults cache");
            }
            
            // Invalidate nearby locations cache
            var nearbyCache = cacheManager.getCache("nearbyLocations");
            if (nearbyCache != null) {
                nearbyCache.clear();
                log.debug("Cleared nearbyLocations cache");
            }
            
        } catch (Exception e) {
            log.warn("Failed to invalidate caches", e);
            // Non-critical, don't fail the event processing
        }
    }
}
