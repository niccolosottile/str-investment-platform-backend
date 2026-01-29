package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Mapper for converting between ScrapingJob domain model and ScrapingJobEntity.
 */
@Component
public class ScrapingJobEntityMapper {

    /**
     * Convert JPA entity to domain model
     */
    public ScrapingJob toDomain(ScrapingJobEntity entity) {
        if (entity == null) {
            return null;
        }

        ScrapingJob job = new ScrapingJob(
            entity.getLocationId(),
            mapPlatformToDomain(entity.getPlatform())
        );

        job.restore(
            entity.getId(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null
        );

        // Handle status transitions based on entity state
        if (entity.getStatus() == ScrapingJobEntity.JobStatus.IN_PROGRESS) {
            job.start();
        } else if (entity.getStatus() == ScrapingJobEntity.JobStatus.COMPLETED && entity.getPropertiesFound() != null) {
            job.start();
            job.complete(entity.getPropertiesFound());
        } else if (entity.getStatus() == ScrapingJobEntity.JobStatus.FAILED) {
            job.start();
            job.fail(entity.getErrorMessage());
        }

        return job;
    }

    /**
     * Convert domain model to JPA entity
     */
    public ScrapingJobEntity toEntity(ScrapingJob domain) {
        if (domain == null) {
            return null;
        }

        return ScrapingJobEntity.builder()
            .id(domain.getId())
            .locationId(domain.getLocationId())
            .platform(mapPlatformToEntity(domain.getPlatform()))
            .status(mapStatusToEntity(domain.getStatus()))
            .propertiesFound(domain.getPropertiesFound())
            .startedAt(domain.getStartedAt() != null ? domain.getStartedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
            .completedAt(domain.getCompletedAt() != null ? domain.getCompletedAt().atZone(ZoneId.systemDefault()).toInstant() : null)
            .errorMessage(domain.getErrorMessage())
            .build();
    }

    /**
     * Update existing entity from domain model
     */
    public void updateEntity(ScrapingJob domain, ScrapingJobEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setStatus(mapStatusToEntity(domain.getStatus()));
        entity.setPropertiesFound(domain.getPropertiesFound());
        entity.setStartedAt(domain.getStartedAt() != null ? domain.getStartedAt().atZone(ZoneId.systemDefault()).toInstant() : null);
        entity.setCompletedAt(domain.getCompletedAt() != null ? domain.getCompletedAt().atZone(ZoneId.systemDefault()).toInstant() : null);
        entity.setErrorMessage(domain.getErrorMessage());
    }

    private ScrapingJob.Platform mapPlatformToDomain(ScrapingJobEntity.Platform entityPlatform) {
        return switch (entityPlatform) {
            case AIRBNB -> ScrapingJob.Platform.AIRBNB;
            case BOOKING -> ScrapingJob.Platform.BOOKING;
            case VRBO -> ScrapingJob.Platform.VRBO;
        };
    }

    private ScrapingJobEntity.Platform mapPlatformToEntity(ScrapingJob.Platform domainPlatform) {
        return switch (domainPlatform) {
            case AIRBNB -> ScrapingJobEntity.Platform.AIRBNB;
            case BOOKING -> ScrapingJobEntity.Platform.BOOKING;
            case VRBO -> ScrapingJobEntity.Platform.VRBO;
        };
    }

    private ScrapingJobEntity.JobStatus mapStatusToEntity(ScrapingJob.JobStatus domainStatus) {
        return switch (domainStatus) {
            case PENDING -> ScrapingJobEntity.JobStatus.PENDING;
            case IN_PROGRESS -> ScrapingJobEntity.JobStatus.IN_PROGRESS;
            case COMPLETED -> ScrapingJobEntity.JobStatus.COMPLETED;
            case FAILED -> ScrapingJobEntity.JobStatus.FAILED;
        };
    }
}
