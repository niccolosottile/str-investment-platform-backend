package com.str.platform.scraping.application.controller;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.scraping.application.service.ScrapingOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
     * Create a new scraping job
     */
    @PostMapping
    @Operation(
        summary = "Create scraping job",
        description = "Creates a new scraping job for the specified location and platform. Job is queued for processing by Python workers."
    )
    @ApiResponse(responseCode = "200", description = "Job created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<ScrapingJobResponse> createScrapingJob(
            @Valid @RequestBody ScrapingJobRequest request
    ) {
        log.info("Creating scraping job: platform={}, coords=({}, {})", 
            request.platform, request.latitude, request.longitude);
        
        Coordinates coordinates = new Coordinates(request.latitude, request.longitude);
        ScrapingJob.Platform platform = ScrapingJob.Platform.valueOf(request.platform.toUpperCase());
        
        ScrapingJob job = orchestrationService.createScrapingJob(
            coordinates, 
            platform, 
            request.radiusKm
        );
        
        return ResponseEntity.ok(toResponse(job));
    }
    
    /**
     * Create scraping jobs for all platforms
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Create batch scraping jobs",
        description = "Creates scraping jobs for all platforms (Airbnb, Booking, VRBO) at once"
    )
    @ApiResponse(responseCode = "200", description = "Jobs created successfully")
    public ResponseEntity<BatchScrapingJobResponse> createBatchScrapingJobs(
            @Valid @RequestBody BatchScrapingJobRequest request
    ) {
        log.info("Creating batch scraping jobs for coords=({}, {})", 
            request.latitude, request.longitude);
        
        Coordinates coordinates = new Coordinates(request.latitude, request.longitude);
        
        List<ScrapingJob> jobs = orchestrationService.createScrapingJobsForAllPlatforms(
            coordinates, 
            request.radiusKm
        );
        
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
     * Get scraping jobs near a location
     */
    @GetMapping("/nearby")
    @Operation(
        summary = "Get nearby scraping jobs",
        description = "Retrieves recent scraping jobs near the specified coordinates"
    )
    public ResponseEntity<List<ScrapingJobResponse>> getNearbyJobs(
            @Parameter(description = "Latitude") @RequestParam double lat,
            @Parameter(description = "Longitude") @RequestParam double lng,
            @Parameter(description = "Radius in km") @RequestParam(defaultValue = "10") int radiusKm
    ) {
        Coordinates coordinates = new Coordinates(lat, lng);
        List<ScrapingJob> jobs = orchestrationService.getScrapingJobsNearLocation(coordinates, radiusKm);
        
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
            job.getLocation().getLatitude(),
            job.getLocation().getLongitude(),
            job.getPlatform().name(),
            job.getRadiusKm(),
            job.getStatus().name(),
            job.getPropertiesFound(),
            job.getStartedAt() != null ? job.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
            job.getCompletedAt() != null ? job.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
            job.getErrorMessage(),
            job.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }
    
    /**
     * Request DTO for creating a scraping job
     */
    @Schema(description = "Request to create a scraping job")
    public record ScrapingJobRequest(
        @Schema(description = "Latitude", example = "40.7128", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,
        
        @Schema(description = "Longitude", example = "-74.0060", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude,
        
        @Schema(description = "Platform to scrape", example = "AIRBNB", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Platform is required")
        String platform,
        
        @Schema(description = "Search radius in kilometers", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Radius is required")
        @Positive(message = "Radius must be positive")
        Integer radiusKm
    ) {}
    
    /**
     * Request DTO for creating batch scraping jobs
     */
    @Schema(description = "Request to create scraping jobs for all platforms")
    public record BatchScrapingJobRequest(
        @Schema(description = "Latitude", example = "40.7128", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
        Double latitude,
        
        @Schema(description = "Longitude", example = "-74.0060", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
        Double longitude,
        
        @Schema(description = "Search radius in kilometers", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Radius is required")
        @Positive(message = "Radius must be positive")
        Integer radiusKm
    ) {}
    
    /**
     * Response DTO for scraping job
     */
    @Schema(description = "Scraping job details")
    public record ScrapingJobResponse(
        @Schema(description = "Job ID")
        UUID id,
        
        @Schema(description = "Latitude")
        Double latitude,
        
        @Schema(description = "Longitude")
        Double longitude,
        
        @Schema(description = "Platform")
        String platform,
        
        @Schema(description = "Search radius in kilometers")
        Integer radiusKm,
        
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
