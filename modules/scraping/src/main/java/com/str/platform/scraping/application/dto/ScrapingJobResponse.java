package com.str.platform.scraping.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for scraping job
 */
@Schema(description = "Scraping job details")
public record ScrapingJobResponse(
    @Schema(description = "Job ID")
    UUID id,
    
    @Schema(description = "Location ID")
    UUID locationId,
    
    @Schema(description = "Platform")
    String platform,
    
    @Schema(description = "Job status")
    String status,
    
    @Schema(description = "Number of properties found")
    Integer propertiesFound,
    
    @Schema(description = "When the job started")
    Instant startedAt,
    
    @Schema(description = "When the job completed")
    Instant completedAt,
    
    @Schema(description = "Error message if failed")
    String errorMessage,
    
    @Schema(description = "When the job was created")
    Instant createdAt
) {}
