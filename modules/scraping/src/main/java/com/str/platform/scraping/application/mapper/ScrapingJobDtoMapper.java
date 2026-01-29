package com.str.platform.scraping.application.mapper;

import com.str.platform.scraping.application.dto.ScrapingJobResponse;
import com.str.platform.scraping.domain.model.ScrapingJob;

/**
 * Mapper for converting ScrapingJob domain objects to DTOs
 */
public class ScrapingJobDtoMapper {
    
    private ScrapingJobDtoMapper() {}
    
    /**
     * Convert domain model to response DTO
     */
    public static ScrapingJobResponse toResponse(ScrapingJob job) {
        return new ScrapingJobResponse(
            job.getId(),
            job.getLocationId(),
            job.getPlatform().name(),
            job.getStatus().name(),
            job.getPropertiesFound(),
            job.getStartedAt() != null ? job.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
            job.getCompletedAt() != null ? job.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
            job.getErrorMessage(),
            job.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }
}
