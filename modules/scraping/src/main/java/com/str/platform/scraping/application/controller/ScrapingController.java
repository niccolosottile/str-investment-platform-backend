package com.str.platform.scraping.application.controller;

import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.application.service.ScrapingOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for scraping job management.
 * Provides endpoints for creating, monitoring, and retrying scraping jobs.
 */
@Slf4j
@RestController
@RequestMapping("/api/scraping/jobs")
@RequiredArgsConstructor
@Tag(name = "Scraping", description = "Scraping job management API")
public class ScrapingController {
    
    private final ScrapingOrchestrationService orchestrationService;
    
    /**
     * Create a new scraping job for an existing Location.
     */
    @PostMapping("/location/{locationId}")
    @Operation(
        summary = "Create scraping job for location",
        description = "Creates a new scraping job for an existing Location ID and platform."
    )
    @ApiResponse(responseCode = "200", description = "Job created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Location not found")
    public ResponseEntity<ScrapingJobResponse> createScrapingJobForLocation(
            @Parameter(description = "Location ID") @PathVariable UUID locationId,
            @Valid @RequestBody ScrapingJobForLocationRequest request
    ) {
        log.info("Creating scraping job for locationId={}: platform={}", locationId, request.platform);

        ScrapingJob.Platform platform = ScrapingJob.Platform.valueOf(request.platform.toUpperCase());
        ScrapingJob job = orchestrationService.createScrapingJobForLocation(locationId, platform);

        return ResponseEntity.ok(toResponse(job));
    }
    
    @PostMapping("/location/{locationId}/batch")
    @Operation(
        summary = "Create batch scraping jobs for location",
        description = "Creates scraping jobs for all platforms (Airbnb, Booking, VRBO) for an existing Location ID."
    )
    @ApiResponse(responseCode = "200", description = "Jobs created successfully")
    @ApiResponse(responseCode = "404", description = "Location not found")
    public ResponseEntity<BatchScrapingJobResponse> createBatchScrapingJobsForLocation(
            @Parameter(description = "Location ID") @PathVariable UUID locationId
    ) {
        log.info("Creating batch scraping jobs for locationId={}", locationId);

        List<ScrapingJob> jobs = orchestrationService.createScrapingJobsForAllPlatformsForLocation(locationId);

        List<ScrapingJobResponse> responses = jobs.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(new BatchScrapingJobResponse(
            responses.size(),
            responses
        ));
    }
    
    /**
     * Get scraping job by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get scraping job",
        description = "Retrieves a scraping job by ID with current status"
    )
    @ApiResponse(responseCode = "200", description = "Job found")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<ScrapingJobResponse> getScrapingJob(
            @Parameter(description = "Job ID") @PathVariable UUID id
    ) {
        ScrapingJob job = orchestrationService.getScrapingJob(id);
        return ResponseEntity.ok(toResponse(job));
    }
    
    /**
     * Get scraping jobs for a specific location
     */
    @GetMapping("/location/{locationId}")
    @Operation(
        summary = "Get scraping jobs for location",
        description = "Retrieves scraping jobs for the specified location"
    )
    @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Location not found")
    public ResponseEntity<List<ScrapingJobResponse>> getJobsByLocation(
            @Parameter(description = "Location ID") @PathVariable UUID locationId
    ) {
        List<ScrapingJob> jobs = orchestrationService.getScrapingJobsByLocation(locationId);
        
        List<ScrapingJobResponse> responses = jobs.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Retry a failed scraping job
     */
    @PostMapping("/{id}/retry")
    @Operation(
        summary = "Retry scraping job",
        description = "Retries a failed scraping job. Only failed jobs can be retried."
    )
    @ApiResponse(responseCode = "200", description = "Job retry initiated")
    @ApiResponse(responseCode = "400", description = "Job cannot be retried")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<ScrapingJobResponse> retryScrapingJob(
            @Parameter(description = "Job ID") @PathVariable UUID id
    ) {
        log.info("Retrying scraping job: {}", id);
        ScrapingJob job = orchestrationService.retryScrapingJob(id);
        return ResponseEntity.ok(toResponse(job));
    }
    
    /**
     * Get pending jobs (monitoring endpoint)
     */
    @GetMapping("/status/pending")
    @Operation(
        summary = "Get pending jobs",
        description = "Lists all jobs waiting to be processed"
    )
    public ResponseEntity<List<ScrapingJobResponse>> getPendingJobs() {
        List<ScrapingJob> jobs = orchestrationService.getPendingJobs();
        
        List<ScrapingJobResponse> responses = jobs.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get in-progress jobs (monitoring endpoint)
     */
    @GetMapping("/status/in-progress")
    @Operation(
        summary = "Get in-progress jobs",
        description = "Lists all jobs currently being processed"
    )
    public ResponseEntity<List<ScrapingJobResponse>> getInProgressJobs() {
        List<ScrapingJob> jobs = orchestrationService.getInProgressJobs();
        
        List<ScrapingJobResponse> responses = jobs.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Convert domain model to response DTO
     */
    private ScrapingJobResponse toResponse(ScrapingJob job) {
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
    
    /**
     * Request DTO for creating a scraping job for an existing Location.
     */
    @Schema(description = "Request to create a scraping job for an existing location")
    public record ScrapingJobForLocationRequest(
        @Schema(description = "Platform to scrape", example = "AIRBNB", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Platform is required")
        String platform
    ) {}
    
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
}
