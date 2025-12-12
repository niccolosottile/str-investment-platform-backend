package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
            entity.getId(),
            entity.getLatitude().doubleValue(),
            entity.getLongitude().doubleValue(),
            mapPlatformToDomain(entity.getPlatform()),
            entity.getRadiusKm()
        );

        // Set status and execution details
        job.updateStatus(
            mapStatusToDomain(entity.getStatus()),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getPropertiesFound(),
            entity.getErrorMessage()
        );

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
            .latitude(BigDecimal.valueOf(domain.getLatitude()))
            .longitude(BigDecimal.valueOf(domain.getLongitude()))
            .platform(mapPlatformToEntity(domain.getPlatform()))
            .radiusKm(domain.getRadiusKm())
            .status(mapStatusToEntity(domain.getStatus()))
            .propertiesFound(domain.getPropertiesFound())
            .startedAt(domain.getStartedAt())
            .completedAt(domain.getCompletedAt())
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
        entity.setStartedAt(domain.getStartedAt());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setErrorMessage(domain.getErrorMessage());
    }

    private ScrapingJob.Platform mapPlatformToDomain(ScrapingJobEntity.Platform entityPlatform) {
        return switch (entityPlatform) {
            case AIRBNB -> ScrapingJob.Platform.AIRBNB;
            case BOOKING_COM -> ScrapingJob.Platform.BOOKING_COM;
            case VRBO -> ScrapingJob.Platform.VRBO;
        };
    }

    private ScrapingJobEntity.Platform mapPlatformToEntity(ScrapingJob.Platform domainPlatform) {
        return switch (domainPlatform) {
            case AIRBNB -> ScrapingJobEntity.Platform.AIRBNB;
            case BOOKING_COM -> ScrapingJobEntity.Platform.BOOKING_COM;
            case VRBO -> ScrapingJobEntity.Platform.VRBO;
        };
    }

    private ScrapingJob.JobStatus mapStatusToDomain(ScrapingJobEntity.JobStatus entityStatus) {
        return switch (entityStatus) {
            case PENDING -> ScrapingJob.JobStatus.PENDING;
            case IN_PROGRESS -> ScrapingJob.JobStatus.IN_PROGRESS;
            case COMPLETED -> ScrapingJob.JobStatus.COMPLETED;
            case FAILED -> ScrapingJob.JobStatus.FAILED;
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
