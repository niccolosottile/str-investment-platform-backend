package com.str.platform.scraping.application.controller;

import com.str.platform.scraping.application.dto.BatchScrapingJobResponse;
import com.str.platform.scraping.application.dto.BatchScrapingRequest;
import com.str.platform.scraping.application.dto.BatchScrapingStatusResponse;
import com.str.platform.scraping.application.dto.ScrapingJobForLocationRequest;
import com.str.platform.scraping.application.dto.ScrapingJobResponse;
import com.str.platform.scraping.application.mapper.ScrapingJobDtoMapper;
import com.str.platform.scraping.application.service.BatchScrapingService;
import com.str.platform.scraping.application.service.ScrapingOrchestrationService;
import com.str.platform.scraping.domain.model.ScrapingJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final BatchScrapingService batchScrapingService;
    
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
        log.info("Creating scraping job for locationId={}: platform={}", locationId, request.platform());

        ScrapingJob.Platform platform = ScrapingJob.Platform.valueOf(request.platform().toUpperCase());
        ScrapingJob job = orchestrationService.createScrapingJobForLocation(locationId, platform);

        return ResponseEntity.ok(ScrapingJobDtoMapper.toResponse(job));
    }

    /**
     * Create batch scraping jobs for all platforms for an existing Location.
     */
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
            .map(ScrapingJobDtoMapper::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(new BatchScrapingJobResponse(
            responses.size(),
            responses
        ));
    }

    /**
     * Orchestrate comprehensive location analysis (deep scrape + price sampling).
     */
    @PostMapping("/location/{locationId}/orchestrate")
    @Operation(
        summary = "Orchestrate comprehensive location analysis",
        description = "Creates FULL_PROFILE jobs for all platforms plus 36 PRICE_SAMPLE jobs (12 months × 3 platforms)"
    )
    @ApiResponse(responseCode = "200", description = "Orchestration completed")
    public ResponseEntity<BatchScrapingJobResponse> orchestrateLocationAnalysis(
            @Parameter(description = "Location ID") @PathVariable UUID locationId
    ) {
        log.info("Orchestrating comprehensive analysis for locationId={}", locationId);

        List<ScrapingJob> jobs = orchestrationService.orchestrateLocationAnalysis(locationId);

        List<ScrapingJobResponse> responses = jobs.stream()
            .map(ScrapingJobDtoMapper::toResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(new BatchScrapingJobResponse(
            responses.size(),
            responses
        ));
    }
    
    /**
     * Get job status by ID
     */
    @GetMapping("/{jobId}")
    @Operation(
        summary = "Get job status",
        description = "Retrieves the current status of a scraping job"
    )
    @ApiResponse(responseCode = "200", description = "Job found")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<ScrapingJobResponse> getJobStatus(
            @Parameter(description = "Job ID") @PathVariable UUID jobId
    ) {
        ScrapingJob job = orchestrationService.getJobById(jobId);
        
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ScrapingJobDtoMapper.toResponse(job));
    }

    /**
     * Retry a failed scraping job
     */
    @PostMapping("/{jobId}/retry")
    @Operation(
        summary = "Retry failed job",
        description = "Resets a failed job to pending and republishes it to the queue"
    )
    @ApiResponse(responseCode = "200", description = "Job retried successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "400", description = "Job is not in failed status")
    public ResponseEntity<ScrapingJobResponse> retryJob(
            @Parameter(description = "Job ID") @PathVariable UUID jobId
    ) {
        try {
            ScrapingJob retriedJob = orchestrationService.retryScrapingJob(jobId);
            return ResponseEntity.ok(ScrapingJobDtoMapper.toResponse(retriedJob));
            
        } catch (IllegalArgumentException e) {
            log.warn("Job not found for retry: {}", jobId);
            return ResponseEntity.notFound().build();
            
        } catch (IllegalStateException e) {
            log.warn("Cannot retry job {}: {}", jobId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
            .map(ScrapingJobDtoMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Start batch scraping for multiple locations
     */
    @PostMapping("/batch/start")
    @Operation(
        summary = "Start batch scraping",
        description = "Initiates batch scraping for multiple locations based on strategy (ALL_LOCATIONS or STALE_ONLY)"
    )
    @ApiResponse(responseCode = "200", description = "Batch scraping started")
    @ApiResponse(responseCode = "400", description = "Invalid request or batch already running")
    public ResponseEntity<UUID> startBatchScraping(
            @Valid @RequestBody BatchScrapingRequest request
    ) {
        log.info("Starting batch scraping: strategy={}, delayMinutes={}", 
            request.strategy(), request.delayMinutes());
        
        try {
            UUID batchId = batchScrapingService.scheduleBatchRefresh(request);
            return ResponseEntity.ok(batchId);
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Failed to start batch scraping: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get batch scraping status
     */
    @GetMapping("/batch/status")
    @Operation(
        summary = "Get batch scraping status",
        description = "Returns the current status of batch scraping operation"
    )
    public ResponseEntity<BatchScrapingStatusResponse> getBatchStatus() {
        BatchScrapingStatusResponse status = batchScrapingService.getBatchProgress();
        return ResponseEntity.ok(status);
    }
}
