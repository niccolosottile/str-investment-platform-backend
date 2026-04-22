package com.str.platform.scraping.infrastructure.messaging;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.scraping.domain.event.ScrapingJobCompletedEvent;
import com.str.platform.scraping.domain.event.ScrapingJobFailedEvent;
import com.str.platform.scraping.domain.model.PropertyAvailability;
import com.str.platform.scraping.domain.model.PriceSample;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyAvailabilityEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.PriceSampleEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.metrics.ScrapingMetricsService;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyAvailabilityRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPriceSampleRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import com.str.platform.shared.event.ScrapingDataUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
@RabbitListener(queues = ScrapingResultConsumer.SCRAPING_RESULT_QUEUE)
public class ScrapingResultConsumer {
    
    private final JpaScrapingJobRepository scrapingJobRepository;
    private final JpaPropertyRepository propertyRepository;
    private final JpaPropertyAvailabilityRepository availabilityRepository;
    private final JpaPriceSampleRepository priceSampleRepository;
    private final LocationService locationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScrapingMetricsService scrapingMetricsService;
    
    public static final String SCRAPING_RESULT_QUEUE = "str.scraping.result.queue";
    
    /**
     * Handle scraping job completed event from Python worker.
     * Updates job status and saves scraped properties with availability and price data.
     */
    @RabbitHandler
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

            scrapingMetricsService.recordJobCompleted(
                event.getJobType(),
                ScrapingJob.Platform.valueOf(jobEntity.getPlatform().name())
            );
            
            // Location-first: jobs must always carry a Location ID
            UUID locationId = jobEntity.getLocationId();
            if (locationId == null) {
                throw new IllegalStateException("Scraping job missing locationId: " + jobEntity.getId());
            }
            
            // Save or update properties with availability and price data
            int savedCount = 0;
            int availabilitySaved = 0;
            int priceSamplesSaved = 0;
            BigDecimal totalSamplePrice = BigDecimal.ZERO;
            int pricedPropertiesCount = 0;
            
            for (ScrapingJobCompletedEvent.PropertyData propData : event.getProperties()) {
                try {
                    // Check if property already exists
                    PropertyEntity.Platform platform = PropertyEntity.Platform.valueOf(propData.getPlatform());
                    PropertyEntity existingProperty = propertyRepository
                        .findByPlatformAndPlatformPropertyId(platform, propData.getPlatformId())
                        .orElse(null);
                    
                    PropertyEntity property;
                    if (existingProperty != null) {
                        // Update existing property
                        updatePropertyEntity(existingProperty, propData, locationId);
                        property = propertyRepository.save(existingProperty);
                        log.debug("Updated existing property: {}", propData.getPlatformId());
                    } else {
                        // Create new property
                        PropertyEntity newProperty = createPropertyEntity(propData, locationId);
                        property = propertyRepository.save(newProperty);
                        log.debug("Created new property: {}", propData.getPlatformId());
                    }
                    savedCount++;
                    
                    // Save availability calendar (for FULL_PROFILE jobs)
                    if (propData.getAvailability() != null && !propData.getAvailability().isEmpty()) {
                        int saved = saveAvailabilityData(property.getId(), propData.getAvailability());
                        availabilitySaved += saved;
                        log.debug("Saved {} availability records for property: {}", saved, propData.getPlatformId());
                    }
                    
                    // Save price sample (for PRICE_SAMPLE jobs)
                    if (propData.getPriceSample() != null) {
                        savePriceSample(property.getId(), propData.getPriceSample());
                        priceSamplesSaved++;
                        totalSamplePrice = totalSamplePrice.add(propData.getPriceSample().getPrice());
                        pricedPropertiesCount++;
                        log.debug("Saved price sample for property: {}", propData.getPlatformId());
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to save property: {}", propData.getPlatformId(), e);
                    // Continue processing other properties
                }
            }
            
            log.info("Saved {} properties, {} availability records, {} price samples for job: {}", 
                savedCount, availabilitySaved, priceSamplesSaved, event.getJobId());

            BigDecimal averagePrice = pricedPropertiesCount == 0
                ? null
                : totalSamplePrice.divide(BigDecimal.valueOf(pricedPropertiesCount), 2, RoundingMode.HALF_UP);

            locationService.updateScrapingData(locationId, savedCount, averagePrice);
            
            // Publish domain event for cache invalidation (handled by analysis module)
            eventPublisher.publishEvent(
                new ScrapingDataUpdatedEvent(jobEntity.getLocationId(), savedCount)
            );
            
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
    @RabbitHandler
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

            scrapingMetricsService.recordJobFailed(
                null,
                ScrapingJob.Platform.valueOf(jobEntity.getPlatform().name()),
                event.getErrorType()
            );
            
            log.info("Marked scraping job as failed: jobId={}", event.getJobId());
            
        } catch (Exception e) {
            log.error("Failed to process scraping job failed event: jobId={}", 
                event.getJobId(), e);
            throw e;
        }
    }
    
    /**
     * Create new property entity from event data.
     */
    private PropertyEntity createPropertyEntity(ScrapingJobCompletedEvent.PropertyData propData, 
                                                UUID locationId) {
        Instant pdpLastScraped = toInstant(propData.getPdpLastScraped());
        Instant availabilityLastScraped =
            propData.getAvailability() != null && !propData.getAvailability().isEmpty()
                ? Instant.now()
                : null;

        return PropertyEntity.builder()
            .locationId(locationId)
            .platform(PropertyEntity.Platform.valueOf(propData.getPlatform()))
            .platformPropertyId(propData.getPlatformId())
            .latitude(java.math.BigDecimal.valueOf(propData.getLatitude()))
            .longitude(java.math.BigDecimal.valueOf(propData.getLongitude()))
            .title(propData.getTitle())
            .propertyType(normalizePropertyType(propData.getPropertyType()))
            .bedrooms(propData.getBedrooms())
            .bathrooms(propData.getBathrooms())
            .guests(propData.getGuests())
            .rating(propData.getRating())
            .reviewCount(propData.getReviewCount())
            .isSuperhost(propData.getIsSuperhost())
            .imageUrl(propData.getImageUrl())
            .propertyUrl(propData.getPropertyUrl())
            .pdpLastScraped(pdpLastScraped)
            .availabilityLastScraped(availabilityLastScraped)
            .build();
    }
    
    /**
     * Update existing property entity with new data
     */
    private void updatePropertyEntity(PropertyEntity entity, ScrapingJobCompletedEvent.PropertyData propData,
                                     UUID locationId) {
        entity.setLocationId(locationId);
        entity.setTitle(propData.getTitle());
        entity.setPropertyType(normalizePropertyType(propData.getPropertyType()));
        entity.setBedrooms(propData.getBedrooms());
        entity.setBathrooms(propData.getBathrooms());
        entity.setGuests(propData.getGuests());
        entity.setRating(propData.getRating());
        entity.setReviewCount(propData.getReviewCount());
        entity.setIsSuperhost(propData.getIsSuperhost());
        entity.setImageUrl(propData.getImageUrl());
        entity.setPropertyUrl(propData.getPropertyUrl());

        Instant pdpLastScraped = toInstant(propData.getPdpLastScraped());
        if (pdpLastScraped != null) {
            entity.setPdpLastScraped(pdpLastScraped);
        }

        if (propData.getAvailability() != null && !propData.getAvailability().isEmpty()) {
            entity.setAvailabilityLastScraped(Instant.now());
        }
    }

    private Instant toInstant(java.time.LocalDateTime dateTime) {
        return dateTime == null
            ? null
            : dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    /**
     * Normalizes raw Airbnb property type strings to canonical domain enum names.
     * Airbnb titles yield prefixes such as "Apartment", "Condo", "Townhouse", "Room", etc.
     * The canonical names stored in the DB must match Property.PropertyType exactly.
     */
    private String normalizePropertyType(String raw) {
        if (raw == null || raw.isBlank()) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.ENTIRE_APARTMENT.name();
        }
        // Already canonical (e.g. re-processing or future scrapers that emit enum names)
        try {
            com.str.platform.scraping.domain.model.Property.PropertyType.valueOf(
                raw.toUpperCase().replace(' ', '_').replace('-', '_'));
            return raw.toUpperCase().replace(' ', '_').replace('-', '_');
        } catch (IllegalArgumentException ignored) {
            // Fall through
        }
        String lower = raw.toLowerCase();
        if (lower.equals("private room") || lower.equals("room") || lower.equals("suite")) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.PRIVATE_ROOM.name();
        }
        if (lower.equals("shared room")) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.SHARED_ROOM.name();
        }
        if (lower.equals("townhouse") || lower.equals("house") || lower.equals("villa")
                || lower.equals("cottage") || lower.equals("cabin") || lower.equals("chalet")
                || lower.equals("bungalow") || lower.equals("farmhouse")) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.ENTIRE_HOUSE.name();
        }
        // Substring fallback for unexpected compound values
        if (lower.contains("private room") || lower.contains("private_room")) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.PRIVATE_ROOM.name();
        }
        if (lower.contains("shared room") || lower.contains("shared_room")) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.SHARED_ROOM.name();
        }
        if (lower.contains("house") || lower.contains("villa") || lower.contains("cottage")
                || lower.contains("cabin") || lower.contains("chalet") || lower.contains("townhouse")
                || lower.contains("bungalow") || lower.contains("farmhouse")) {
            return com.str.platform.scraping.domain.model.Property.PropertyType.ENTIRE_HOUSE.name();
        }
        // Default: apartments, condos, flats, lofts, studios, and anything unrecognised
        return com.str.platform.scraping.domain.model.Property.PropertyType.ENTIRE_APARTMENT.name();
    }
    
    /**
     * Save availability calendar data for a property.
     * Creates one record per month with aggregated availability statistics.
     * 
     * @param propertyId The property UUID
     * @param availabilityList List of monthly availability data from scraper
     * @return Number of records saved
     */
    private int saveAvailabilityData(UUID propertyId, java.util.List<PropertyAvailability> availabilityList) {
        int savedCount = 0;
        Instant scrapedAt = Instant.now();
        
        for (PropertyAvailability availability : availabilityList) {
            try {
                String month = availability.getMonth().toString();
                PropertyAvailabilityEntity entity = availabilityRepository
                    .findFirstByPropertyIdAndMonth(propertyId, month)
                    .orElseGet(() -> PropertyAvailabilityEntity.builder()
                        .propertyId(propertyId)
                        .month(month)
                        .build());

                entity.setTotalDays(availability.getTotalDays());
                entity.setAvailableDays(availability.getAvailableDays());
                entity.setBookedDays(availability.getBookedDays());
                entity.setBlockedDays(availability.getBlockedDays());
                entity.setEstimatedOccupancy(java.math.BigDecimal.valueOf(availability.getEstimatedOccupancy()));
                entity.setScrapedAt(scrapedAt);
                
                availabilityRepository.save(entity);
                savedCount++;
                
            } catch (Exception e) {
                log.error("Failed to save availability record for property: {}, month: {}", 
                    propertyId, availability.getMonth(), e);
                // Continue with next record
            }
        }
        
        return savedCount;
    }
    
    /**
     * Save price sample data for a property.
     * Each sample represents pricing for a specific date range and stay duration.
     * 
     * @param propertyId The property UUID
     * @param priceSample The price sample from scraper
     */
    private void savePriceSample(UUID propertyId, PriceSample priceSample) {
        try {
            PriceSampleEntity entity = priceSampleRepository
                .findByPropertyIdAndSearchDateStartAndSearchDateEnd(
                    propertyId,
                    priceSample.getSearchDateStart(),
                    priceSample.getSearchDateEnd()
                )
                .orElseGet(() -> PriceSampleEntity.builder()
                    .propertyId(propertyId)
                    .searchDateStart(priceSample.getSearchDateStart())
                    .searchDateEnd(priceSample.getSearchDateEnd())
                    .build());

            entity.setPrice(priceSample.getPrice());
            entity.setCurrency(priceSample.getCurrency());
            entity.setNumberOfNights(priceSample.getNumberOfNights());
            entity.setSampledAt(priceSample.getSampledAt());
            
            priceSampleRepository.save(entity);
            
        } catch (Exception e) {
            log.error("Failed to save price sample for property: {}, dates: {} to {}", 
                propertyId, priceSample.getSearchDateStart(), priceSample.getSearchDateEnd(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }
}
