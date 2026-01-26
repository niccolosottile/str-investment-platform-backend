package com.str.platform.scraping.application.service;

import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.BoundingBox;
import com.str.platform.location.domain.model.Coordinates;
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
        Location location = locationService.getById(locationId);
        Coordinates coordinates = location.getCoordinates();

        log.info("Creating {} job for locationId={} ({}): platform={}, coords=({}, {})",
            jobType, locationId, location.getName(), platform, 
            coordinates.getLatitude(), coordinates.getLongitude());

        ScrapingJob job = new ScrapingJob(coordinates, platform, 0);

        ScrapingJobEntity entity = scrapingJobMapper.toEntity(job);
        entity.setLocationId(locationId);
        entity = scrapingJobRepository.save(entity);

        ScrapingJob savedJob = scrapingJobMapper.toDomain(entity);

        // Publish event to RabbitMQ for Python scraper
        try {
            BoundingBox bbox = location.getBoundingBox();
            
            if (bbox == null) {
                log.warn("Location {} has no bounding box - scraping may be less accurate", locationId);
            }
            
            ScrapingJobCreatedEvent.ScrapingJobCreatedEventBuilder eventBuilder = 
                ScrapingJobCreatedEvent.builder()
                    .jobId(savedJob.getId())
                    .locationId(locationId)
                    .locationName(location.getName())
                    .jobType(jobType)
                    .platform(platform)
                    .searchDateStart(java.time.LocalDate.now().plusDays(30))
                    .searchDateEnd(java.time.LocalDate.now().plusDays(37))
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
        
        // Get location for proper event
        Location location = locationService.getById(entity.getLocationId());
        BoundingBox bbox = location.getBoundingBox();
        
        // Publish retry event
        try {
            ScrapingJobCreatedEvent.ScrapingJobCreatedEventBuilder eventBuilder = 
                ScrapingJobCreatedEvent.builder()
                    .jobId(domain.getId())
                    .locationId(entity.getLocationId())
                    .locationName(location.getName())
                    .jobType(com.str.platform.scraping.domain.model.JobType.FULL_PROFILE)
                    .platform(domain.getPlatform())
                    .searchDateStart(java.time.LocalDate.now().plusDays(30))
                    .searchDateEnd(java.time.LocalDate.now().plusDays(37))
                    .occurredAt(LocalDateTime.now());
            
            if (bbox != null) {
                eventBuilder
                    .boundingBoxSwLng(bbox.getSouthWestLongitude())
                    .boundingBoxSwLat(bbox.getSouthWestLatitude())
                    .boundingBoxNeLng(bbox.getNorthEastLongitude())
                    .boundingBoxNeLat(bbox.getNorthEastLatitude());
            }
            
            ScrapingJobCreatedEvent event = eventBuilder.build();
            
            jobPublisher.publishJobCreated(event);
            
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
