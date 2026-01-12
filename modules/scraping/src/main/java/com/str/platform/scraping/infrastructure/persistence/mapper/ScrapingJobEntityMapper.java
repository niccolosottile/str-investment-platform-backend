package com.str.platform.scraping.infrastructure.persistence.mapper;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.infrastructure.persistence.entity.ScrapingJobEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

        Coordinates location = new Coordinates(
            entity.getLatitude().doubleValue(),
            entity.getLongitude().doubleValue()
        );

        ScrapingJob job = new ScrapingJob(
            location,
            mapPlatformToDomain(entity.getPlatform()),
            entity.getRadiusKm()
        );

        // Set ID using reflection since BaseEntity doesn't expose setter
        try {
            java.lang.reflect.Field idField = com.str.platform.shared.domain.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(job, entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set scraping job ID", e);
        }

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
            .latitude(BigDecimal.valueOf(domain.getLocation().getLatitude()))
            .longitude(BigDecimal.valueOf(domain.getLocation().getLongitude()))
            .platform(mapPlatformToEntity(domain.getPlatform()))
            .radiusKm(domain.getRadiusKm())
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
