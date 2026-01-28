package com.str.platform.scraping.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for batch scraping jobs
 */
@Schema(description = "Batch scraping job creation result")
public record BatchScrapingJobResponse(
    @Schema(description = "Number of jobs created")
    Integer jobsCreated,
    
    @Schema(description = "List of created jobs")
    List<ScrapingJobResponse> jobs
) {}
