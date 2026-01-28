package com.str.platform.scraping.application.service;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.BoundingBox;
import com.str.platform.location.domain.model.Location;
import com.str.platform.scraping.domain.event.ScrapingJobCreatedEvent;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.messaging.ScrapingJobPublisher;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.mapper.ScrapingJobEntityMapper;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestration service for scraping jobs.
 * Manages job lifecycle: creation, status tracking, timeouts, retries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingOrchestrationService {
    
    private final JpaScrapingJobRepository scrapingJobRepository;
    private final ScrapingJobEntityMapper scrapingJobMapper;
    private final ScrapingJobPublisher jobPublisher;
    private final LocationService locationService;
    
    /**
     * Create and publish a scraping job tied to an existing Location.
     */
    @Transactional
    public ScrapingJob createScrapingJobForLocation(
            UUID locationId,
            ScrapingJob.Platform platform
    ) {
        return createScrapingJobForLocation(locationId, platform, 
            com.str.platform.scraping.domain.model.JobType.FULL_PROFILE);
    }
    
    /**
     * Create and publish a scraping job tied to an existing Location.
     * Supports both FULL_PROFILE and PRICE_SAMPLE job types.
     */
    @Transactional
    public ScrapingJob createScrapingJobForLocation(
            UUID locationId,
            ScrapingJob.Platform platform,
            com.str.platform.scraping.domain.model.JobType jobType
    ) {
        // Validate location exists
        Location location = locationService.getById(locationId);

        log.info("Creating {} job for locationId={} ({}): platform={}",
            jobType, locationId, location.getName(), platform);

        ScrapingJob job = new ScrapingJob(locationId, platform);

        ScrapingJobEntity entity = scrapingJobMapper.toEntity(job);
        entity = scrapingJobRepository.save(entity);

        ScrapingJob savedJob = scrapingJobMapper.toDomain(entity);

        // Publish event to RabbitMQ for Python scraper
        try {
            publishJobEvent(
                savedJob,
                locationId,
                jobType,
                java.time.LocalDate.now().plusDays(30),
                java.time.LocalDate.now().plusDays(37)
            );

            // Mark job as in progress
            savedJob.start();
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);

            log.info("Successfully created and published {} job: {}", jobType, savedJob.getId());
        } catch (Exception e) {
            log.error("Failed to publish scraping job: {}", savedJob.getId(), e);

            savedJob.fail("Failed to publish job to queue: " + e.getMessage());
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);
            throw new RuntimeException("Failed to create scraping job", e);
        }

        return savedJob;
    }

    /**
     * Orchestrate comprehensive location analysis.
     * Creates both deep profile scraping and price sampling jobs.
     * 
     * @param locationId Location to analyze
     * @return List of all created jobs (FULL_PROFILE + PRICE_SAMPLE jobs)
     */
    @Transactional
    public List<ScrapingJob> orchestrateLocationAnalysis(UUID locationId) {
        log.info("Orchestrating comprehensive analysis for locationId={}", locationId);
        
        List<ScrapingJob> allJobs = new ArrayList<>();
        
        // Step 1: Initiate deep scraping for all platforms (FULL_PROFILE with availability)
        List<ScrapingJob> deepScrapeJobs = initiateDeepScrape(locationId);
        allJobs.addAll(deepScrapeJobs);
        
        // Step 2: Schedule price sampling jobs for historical data collection
        List<ScrapingJob> priceSampleJobs = schedulePriceSampling(locationId);
        allJobs.addAll(priceSampleJobs);
        
        log.info("Orchestration complete for locationId={}: {} deep scrape jobs, {} price sample jobs",
            locationId, deepScrapeJobs.size(), priceSampleJobs.size());
        
        return allJobs;
    }
    
    /**
     * Initiate deep scraping (FULL_PROFILE) for all platforms.
     * Retrieves property details and availability calendars.
     * 
     * @param locationId Location to scrape
     * @return List of created FULL_PROFILE jobs
     */
    @Transactional
    public List<ScrapingJob> initiateDeepScrape(UUID locationId) {
        log.info("Initiating deep scrape for locationId={}", locationId);
        
        return createScrapingJobsForAllPlatformsForLocation(
            locationId, 
            com.str.platform.scraping.domain.model.JobType.FULL_PROFILE
        );
    }
    
    /**
     * Schedule price sampling jobs for longitudinal price data collection.
     * Creates multiple PRICE_SAMPLE jobs spread across future periods.
     * 
     * Strategy: 12 monthly samples over the next year
     * - Each sample: 7-night stay
     * - Starting 30 days from now (to capture near-term bookings)
     * 
     * @param locationId Location to sample
     * @return List of created PRICE_SAMPLE jobs
     */
    @Transactional
    public List<ScrapingJob> schedulePriceSampling(UUID locationId) {
        log.info("Scheduling price sampling for locationId={}", locationId);
        
        List<DateRange> periods = generatePriceSamplePeriods();
        List<ScrapingJob> jobs = new ArrayList<>();
        
        // Create one PRICE_SAMPLE job per platform per period
        for (DateRange period : periods) {
            for (ScrapingJob.Platform platform : ScrapingJob.Platform.values()) {
                try {
                    ScrapingJob job = createPriceSampleJob(locationId, platform, period);
                    jobs.add(job);
                    
                    log.debug("Created PRICE_SAMPLE job for locationId={}, platform={}, dates={} to {}",
                        locationId, platform, period.start(), period.end());
                        
                } catch (Exception e) {
                    log.error("Failed to create PRICE_SAMPLE job for locationId={}, platform={}, period={}",
                        locationId, platform, period, e);
                    // Continue with other jobs
                }
            }
        }
        
        log.info("Created {} PRICE_SAMPLE jobs across {} periods for locationId={}",
            jobs.size(), periods.size(), locationId);
        
        return jobs;
    }
    
    /**
     * Generate date ranges for price sampling.
     * 
     * Strategy: 12 monthly samples
     * - Start: 30 days from now
     * - Duration: 7 nights each
     * - Interval: 30 days between samples
     * 
     * @return List of date ranges for sampling
     */
    private List<DateRange> generatePriceSamplePeriods() {
        List<DateRange> periods = new ArrayList<>();
        java.time.LocalDate baseDate = java.time.LocalDate.now().plusDays(30);
        
        for (int i = 0; i < 12; i++) {
            java.time.LocalDate start = baseDate.plusDays(i * 30L);
            java.time.LocalDate end = start.plusDays(7);
            periods.add(new DateRange(start, end));
        }
        
        log.debug("Generated {} price sample periods from {} to {}",
            periods.size(), 
            periods.get(0).start(), 
            periods.get(periods.size() - 1).end());
        
        return periods;
    }
    
    /**
     * Create a single PRICE_SAMPLE job for a specific date range.
     * 
     * @param locationId Location to sample
     * @param platform Platform to scrape
     * @param dateRange Date range for the sample
     * @return Created scraping job
     */
    @Transactional
    private ScrapingJob createPriceSampleJob(
            UUID locationId,
            ScrapingJob.Platform platform,
            DateRange dateRange
    ) {
        // Validate location exists
        locationService.getById(locationId);

        log.debug("Creating PRICE_SAMPLE job for locationId={}: platform={}, dates={} to {}",
            locationId, platform, dateRange.start(), dateRange.end());

        // Create domain object
        ScrapingJob job = new ScrapingJob(locationId, platform);

        // Convert to entity and save
        ScrapingJobEntity entity = scrapingJobMapper.toEntity(job);
        entity = scrapingJobRepository.save(entity);

        ScrapingJob savedJob = scrapingJobMapper.toDomain(entity);

        // Publish event to RabbitMQ for Python scraper
        try {
            publishJobEvent(
                savedJob,
                locationId,
                com.str.platform.scraping.domain.model.JobType.PRICE_SAMPLE,
                dateRange.start(),
                dateRange.end()
            );

            // Mark job as in progress
            savedJob.start();
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);

            log.debug("Successfully created and published PRICE_SAMPLE job: {}", savedJob.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish PRICE_SAMPLE job: {}", savedJob.getId(), e);

            savedJob.fail("Failed to publish job to queue: " + e.getMessage());
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);
            throw new RuntimeException("Failed to create PRICE_SAMPLE job", e);
        }

        return savedJob;
    }
    
    /**
     * Simple record to represent a date range for price sampling.
     */
    private record DateRange(java.time.LocalDate start, java.time.LocalDate end) {}
    
    /**
     * Build and publish scraping job event to RabbitMQ.
     * Centralizes event creation logic to avoid duplication.
     */
    private void publishJobEvent(
            ScrapingJob job,
            UUID locationId,
            com.str.platform.scraping.domain.model.JobType jobType,
            java.time.LocalDate searchDateStart,
            java.time.LocalDate searchDateEnd
    ) {
        Location location = locationService.getById(locationId);
        BoundingBox bbox = location.getBoundingBox();
        
        if (bbox == null) {
            log.warn("Location {} has no bounding box - scraping may be less accurate", locationId);
        }
        
        ScrapingJobCreatedEvent.ScrapingJobCreatedEventBuilder eventBuilder = 
            ScrapingJobCreatedEvent.builder()
                .jobId(job.getId())
                .locationId(locationId)
                .locationName(location.getName())
                .jobType(jobType)
                .platform(job.getPlatform())
                .searchDateStart(searchDateStart)
                .searchDateEnd(searchDateEnd)
                .occurredAt(LocalDateTime.now());
        
        // Add bounding box if available
        if (bbox != null) {
            eventBuilder
                .boundingBoxSwLng(bbox.getSouthWestLongitude())
                .boundingBoxSwLat(bbox.getSouthWestLatitude())
                .boundingBoxNeLng(bbox.getNorthEastLongitude())
                .boundingBoxNeLat(bbox.getNorthEastLatitude());
        }
        
        ScrapingJobCreatedEvent event = eventBuilder.build();
        jobPublisher.publishJobCreated(event);
    }


    /**
     * Create scraping jobs for all platforms for an existing Location.
     */
    @Transactional
    public List<ScrapingJob> createScrapingJobsForAllPlatformsForLocation(UUID locationId) {
        return createScrapingJobsForAllPlatformsForLocation(locationId, 
            com.str.platform.scraping.domain.model.JobType.FULL_PROFILE);
    }
    
    /**
     * Create scraping jobs for all platforms for an existing Location with specific job type.
     */
    @Transactional
    public List<ScrapingJob> createScrapingJobsForAllPlatformsForLocation(
            UUID locationId,
            com.str.platform.scraping.domain.model.JobType jobType
    ) {
        log.info("Creating {} jobs for all platforms for locationId={}", jobType, locationId);

        List<ScrapingJob> jobs = new ArrayList<>();
        for (ScrapingJob.Platform platform : ScrapingJob.Platform.values()) {
            try {
                jobs.add(createScrapingJobForLocation(locationId, platform, jobType));
            } catch (Exception e) {
                log.error("Failed to create {} job for locationId={} platform={}", 
                    jobType, locationId, platform, e);
            }
        }

        log.info("Created {} scraping jobs for locationId={}", jobs.size(), locationId);
        return jobs;
    }
    
    /**
     * Get scraping job by ID
     */
    public ScrapingJob getScrapingJob(UUID jobId) {
        ScrapingJobEntity entity = scrapingJobRepository.findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("ScrapingJob", jobId));
        return scrapingJobMapper.toDomain(entity);
    }
    
    /**
     * Get scraping jobs for a specific location
     */
    public List<ScrapingJob> getScrapingJobsByLocation(UUID locationId) {
        List<ScrapingJobEntity> entities = scrapingJobRepository.findByLocationId(locationId);
        return entities.stream()
            .map(scrapingJobMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all pending jobs
     */
    public List<ScrapingJob> getPendingJobs() {
        return scrapingJobRepository.findByStatus(ScrapingJobEntity.JobStatus.PENDING)
            .stream()
            .map(scrapingJobMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all in-progress jobs
     */
    public List<ScrapingJob> getInProgressJobs() {
        return scrapingJobRepository.findByStatus(ScrapingJobEntity.JobStatus.IN_PROGRESS)
            .stream()
            .map(scrapingJobMapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Handle timed-out jobs (mark as failed after specified minutes)
     */
    @Transactional
    public int handleTimedOutJobs(int timeoutMinutes) {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(timeoutMinutes));
        
        List<ScrapingJobEntity> timedOutJobs = scrapingJobRepository.findTimedOutJobs(threshold);
        
        log.info("Found {} timed-out jobs (threshold: {} minutes)", timedOutJobs.size(), timeoutMinutes);
        
        for (ScrapingJobEntity job : timedOutJobs) {
            job.setStatus(ScrapingJobEntity.JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage("Job timed out after " + timeoutMinutes + " minutes");
            scrapingJobRepository.save(job);
            
            log.warn("Marked job as timed out: {}", job.getId());
        }
        
        return timedOutJobs.size();
    }
    
    /**
     * Retry a failed scraping job
     */
    @Transactional
    public ScrapingJob retryScrapingJob(UUID jobId) {
        log.info("Retrying scraping job: {}", jobId);
        
        ScrapingJobEntity entity = scrapingJobRepository.findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("ScrapingJob", jobId));
        
        if (entity.getStatus() != ScrapingJobEntity.JobStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed jobs. Current status: " + entity.getStatus());
        }
        
        // Reset job status
        entity.setStatus(ScrapingJobEntity.JobStatus.PENDING);
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setErrorMessage(null);
        entity.setPropertiesFound(null);
        entity = scrapingJobRepository.save(entity);
        
        // Convert to domain and publish retry
        ScrapingJob domain = scrapingJobMapper.toDomain(entity);
        
        // Publish retry event
        try {
            publishJobEvent(
                domain,
                entity.getLocationId(),
                com.str.platform.scraping.domain.model.JobType.FULL_PROFILE,
                java.time.LocalDate.now().plusDays(30),
                java.time.LocalDate.now().plusDays(37)
            );
            
            // Mark as in progress
            domain.start();
            scrapingJobMapper.updateEntity(domain, entity);
            scrapingJobRepository.save(entity);
            
            log.info("Successfully retried scraping job: {}", jobId);
            
        } catch (Exception e) {
            log.error("Failed to retry scraping job: {}", jobId, e);
            domain.fail("Failed to publish retry: " + e.getMessage());
            scrapingJobMapper.updateEntity(domain, entity);
            scrapingJobRepository.save(entity);
            throw new RuntimeException("Failed to retry scraping job", e);
        }
        
        return domain;
    }
}
