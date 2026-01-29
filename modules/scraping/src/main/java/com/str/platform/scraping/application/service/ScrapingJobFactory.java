package com.str.platform.scraping.application.service;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import com.str.platform.scraping.infrastructure.persistence.mapper.ScrapingJobEntityMapper;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaScrapingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Creates and persists scraping jobs.
 */
@Service
@RequiredArgsConstructor
public class ScrapingJobFactory {

    private final JpaScrapingJobRepository scrapingJobRepository;
    private final ScrapingJobEntityMapper scrapingJobMapper;

    public CreatedJob createJob(UUID locationId, ScrapingJob.Platform platform) {
        ScrapingJob job = new ScrapingJob(locationId, platform);
        ScrapingJobEntity entity = scrapingJobMapper.toEntity(job);
        entity = scrapingJobRepository.save(entity);
        ScrapingJob savedJob = scrapingJobMapper.toDomain(entity);
        return new CreatedJob(savedJob, entity);
    }

    public record CreatedJob(ScrapingJob job, ScrapingJobEntity entity) {}
}
