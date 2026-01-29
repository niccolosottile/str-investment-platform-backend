package com.str.platform.scraping.application.service;

import com.str.platform.scraping.domain.model.ScrapingJob;
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
    private final ScrapingJobFactory scrapingJobFactory;
    private final ScrapingJobPublisherService scrapingJobPublisherService;
    private final PriceSamplingPlanner priceSamplingPlanner;

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
        log.info("Creating {} job for locationId={}: platform={}", jobType, locationId, platform);

        PriceSamplingPlanner.DateRange defaultRange = priceSamplingPlanner.defaultSearchRange();
        return createAndPublishJob(locationId, platform, jobType, defaultRange);
    }

    /**
     * Orchestrate comprehensive location analysis.
     * Creates both deep profile scraping and price sampling jobs.
     */
    @Transactional
    public List<ScrapingJob> orchestrateLocationAnalysis(UUID locationId) {
        log.info("Orchestrating comprehensive analysis for locationId={}", locationId);

        List<ScrapingJob> allJobs = new ArrayList<>();

        List<ScrapingJob> deepScrapeJobs = initiateDeepScrape(locationId);
        allJobs.addAll(deepScrapeJobs);

        List<ScrapingJob> priceSampleJobs = schedulePriceSampling(locationId);
        allJobs.addAll(priceSampleJobs);

        log.info("Orchestration complete for locationId={}: {} deep scrape jobs, {} price sample jobs",
            locationId, deepScrapeJobs.size(), priceSampleJobs.size());

        return allJobs;
    }

    /**
     * Initiate deep scraping (FULL_PROFILE) for all platforms.
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
     */
    @Transactional
    public List<ScrapingJob> schedulePriceSampling(UUID locationId) {
        log.info("Scheduling price sampling for locationId={}", locationId);

        List<PriceSamplingPlanner.DateRange> periods = priceSamplingPlanner.generatePriceSamplePeriods();
        List<ScrapingJob> jobs = new ArrayList<>();

        for (PriceSamplingPlanner.DateRange period : periods) {
            for (ScrapingJob.Platform platform : ScrapingJob.Platform.values()) {
                try {
                    ScrapingJob job = createPriceSampleJob(locationId, platform, period);
                    jobs.add(job);

                    log.debug("Created PRICE_SAMPLE job for locationId={}, platform={}, dates={} to {}",
                        locationId, platform, period.start(), period.end());

                } catch (Exception e) {
                    log.error("Failed to create PRICE_SAMPLE job for locationId={}, platform={}, period={}",
                        locationId, platform, period, e);
                }
            }
        }

        log.info("Created {} PRICE_SAMPLE jobs across {} periods for locationId={}",
            jobs.size(), periods.size(), locationId);

        return jobs;
    }

    /**
     * Create a single PRICE_SAMPLE job for a specific date range.
     */
    @Transactional
    private ScrapingJob createPriceSampleJob(
            UUID locationId,
            ScrapingJob.Platform platform,
            PriceSamplingPlanner.DateRange dateRange
    ) {
        log.debug("Creating PRICE_SAMPLE job for locationId={}: platform={}, dates={} to {}",
            locationId, platform, dateRange.start(), dateRange.end());

        return createAndPublishJob(
            locationId,
            platform,
            com.str.platform.scraping.domain.model.JobType.PRICE_SAMPLE,
            dateRange
        );
    }

    private ScrapingJob createAndPublishJob(
            UUID locationId,
            ScrapingJob.Platform platform,
            com.str.platform.scraping.domain.model.JobType jobType,
            PriceSamplingPlanner.DateRange dateRange
    ) {
        ScrapingJobFactory.CreatedJob created = scrapingJobFactory.createJob(locationId, platform);
        ScrapingJob savedJob = created.job();
        ScrapingJobEntity entity = created.entity();

        try {
            scrapingJobPublisherService.publishJobCreated(
                savedJob,
                locationId,
                jobType,
                dateRange.start(),
                dateRange.end()
            );

            savedJob.start();
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);

            log.debug("Successfully created and published {} job: {}", jobType, savedJob.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} job: {}", jobType, savedJob.getId(), e);

            savedJob.fail("Failed to publish job to queue: " + e.getMessage());
            scrapingJobMapper.updateEntity(savedJob, entity);
            scrapingJobRepository.save(entity);
            throw new RuntimeException("Failed to create scraping job", e);
        }

        return savedJob;
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
        return scrapingJobRepository.findById(jobId)
            .map(scrapingJobMapper::toDomain)
            .orElseThrow(() -> new EntityNotFoundException("ScrapingJob", jobId));
    }

    /**
     * Get scraping jobs for a specific location
     */
    public List<ScrapingJob> getScrapingJobsByLocation(UUID locationId) {
        return scrapingJobRepository.findByLocationId(locationId)
            .stream()
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

        entity.setStatus(ScrapingJobEntity.JobStatus.PENDING);
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setErrorMessage(null);
        entity.setPropertiesFound(null);
        entity = scrapingJobRepository.save(entity);

        ScrapingJob domain = scrapingJobMapper.toDomain(entity);

        try {
            PriceSamplingPlanner.DateRange defaultRange = priceSamplingPlanner.defaultSearchRange();
            scrapingJobPublisherService.publishJobCreated(
                domain,
                entity.getLocationId(),
                com.str.platform.scraping.domain.model.JobType.FULL_PROFILE,
                defaultRange.start(),
                defaultRange.end()
            );

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
