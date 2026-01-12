package com.str.platform.scraping.application.service;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.application.service.LocationService;
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
    
    private static final int DEFAULT_RADIUS_KM = 5;
    
    /**
     * Create and publish a new scraping job.
     * Returns the created domain model with ID.
     */
    @Transactional
    public ScrapingJob createScrapingJob(
            Coordinates coordinates,
            ScrapingJob.Platform platform,
            int radiusKm
    ) {
        // Location-first: resolve (or create) a Location, then run a location-based job.
        var location = locationService.findOrCreateByCoordinates(
            coordinates.getLatitude(),
            coordinates.getLongitude()
        );
        return createScrapingJobForLocation(location.getId(), platform, radiusKm);
    }

    /**
     * Create and publish a new scraping job tied to an existing Location.
     * The job retains the Location ID for consistent result ingestion.
     */
    @Transactional
    public ScrapingJob createScrapingJobForLocation(
            UUID locationId,
            ScrapingJob.Platform platform,
            int radiusKm
    ) {
        Location location = locationService.getById(locationId);
        Coordinates coordinates = location.getCoordinates();

        log.info("Creating scraping job for locationId={}: platform={}, coords=({}, {}), radius={}km",
            locationId, platform, coordinates.getLatitude(), coordinates.getLongitude(), radiusKm);

        // Create domain object
        ScrapingJob job = new ScrapingJob(coordinates, platform, radiusKm);

        // Convert to entity, set locationId, and save
        ScrapingJobEntity entity = scrapingJobMapper.toEntity(job);
        entity.setLocationId(locationId);
        entity = scrapingJobRepository.save(entity);

        ScrapingJob savedJob = scrapingJobMapper.toDomain(entity);

        // Publish event to RabbitMQ for Python scraper
        try {
            ScrapingJobCreatedEvent event = new ScrapingJobCreatedEvent(
                savedJob.getId(),
                locationId,
                coordinates.getLatitude(),
                coordinates.getLongitude(),
                platform,
                radiusKm,
                LocalDateTime.now()
            );

            jobPublisher.publishJobCreated(event);

            // Mark job as in progress (update only mutable fields to preserve locationId)
            savedJob.start();
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);

            log.info("Successfully created and published scraping job: {}", savedJob.getId());
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
     * Create scraping jobs for all platforms (Airbnb, Booking, VRBO).
     * Returns list of created jobs.
     */
    @Transactional
    public List<ScrapingJob> createScrapingJobsForAllPlatforms(Coordinates coordinates) {
        return createScrapingJobsForAllPlatforms(coordinates, DEFAULT_RADIUS_KM);
    }
    
    /**
     * Create scraping jobs for all platforms with custom radius.
     */
    @Transactional
    public List<ScrapingJob> createScrapingJobsForAllPlatforms(
            Coordinates coordinates, 
            int radiusKm
    ) {
        log.info("Creating scraping jobs for all platforms at ({}, {}), radius={}km",
            coordinates.getLatitude(), coordinates.getLongitude(), radiusKm);
        
        List<ScrapingJob> jobs = new ArrayList<>();
        
        for (ScrapingJob.Platform platform : ScrapingJob.Platform.values()) {
            try {
                ScrapingJob job = createScrapingJob(coordinates, platform, radiusKm);
                jobs.add(job);
            } catch (Exception e) {
                log.error("Failed to create scraping job for platform: {}", platform, e);
                // Continue with other platforms
            }
        }
        
        log.info("Created {} scraping jobs", jobs.size());
        return jobs;
    }

    /**
     * Create scraping jobs for all platforms for an existing Location.
     */
    @Transactional
    public List<ScrapingJob> createScrapingJobsForAllPlatformsForLocation(
            UUID locationId,
            int radiusKm
    ) {
        log.info("Creating scraping jobs for all platforms for locationId={}, radius={}km", locationId, radiusKm);

        List<ScrapingJob> jobs = new ArrayList<>();
        for (ScrapingJob.Platform platform : ScrapingJob.Platform.values()) {
            try {
                jobs.add(createScrapingJobForLocation(locationId, platform, radiusKm));
            } catch (Exception e) {
                log.error("Failed to create scraping job for locationId={} platform={}", locationId, platform, e);
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
     * Get scraping jobs for a specific location (within radius)
     */
    public List<ScrapingJob> getScrapingJobsNearLocation(Coordinates coordinates, int radiusKm) {
        // Query jobs within bounding box
        List<ScrapingJobEntity> entities = scrapingJobRepository.findRecentJobs(
            Instant.now().minus(Duration.ofDays(7))
        );
        
        // Filter by distance and convert to domain
        return entities.stream()
            .filter(entity -> {
                double distance = calculateDistance(
                    coordinates.getLatitude(), coordinates.getLongitude(),
                    entity.getLatitude().doubleValue(), entity.getLongitude().doubleValue()
                );
                return distance <= radiusKm;
            })
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
        Coordinates coords = domain.getLocation();
        
        // Publish retry event
        try {
            ScrapingJobCreatedEvent event = new ScrapingJobCreatedEvent(
                domain.getId(),
                entity.getLocationId(),
                coords.getLatitude(),
                coords.getLongitude(),
                domain.getPlatform(),
                domain.getRadiusKm(),
                LocalDateTime.now()
            );
            
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
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
