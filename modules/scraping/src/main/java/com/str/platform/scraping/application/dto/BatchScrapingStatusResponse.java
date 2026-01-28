package com.str.platform.scraping.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for batch scraping status.
 */
@Schema(description = "Status of ongoing or completed batch scraping operation")
public record BatchScrapingStatusResponse(
    
    @Schema(description = "Batch identifier")
    @JsonProperty("batchId")
    UUID batchId,
    
    @Schema(description = "Current batch status")
    @JsonProperty("status")
    BatchStatus status,
    
    @Schema(description = "Total number of locations to process")
    @JsonProperty("totalLocations")
    int totalLocations,
    
    @Schema(description = "Number of locations completed")
    @JsonProperty("completedLocations")
    int completedLocations,
    
    @Schema(description = "Number of locations that failed")
    @JsonProperty("failedLocations")
    int failedLocations,
    
    @Schema(description = "Currently processing location ID")
    @JsonProperty("currentLocationId")
    UUID currentLocationId,
    
    @Schema(description = "Batch start time")
    @JsonProperty("startTime")
    Instant startTime,
    
    @Schema(description = "Estimated completion time")
    @JsonProperty("estimatedCompletionTime")
    Instant estimatedCompletionTime,
    
    @Schema(description = "Delay between locations in minutes")
    @JsonProperty("delayMinutes")
    int delayMinutes,
    
    @Schema(description = "Progress percentage")
    @JsonProperty("progressPercentage")
    double progressPercentage
) {
    
    public enum BatchStatus {
        NOT_STARTED,
        RUNNING,
        COMPLETED,
        FAILED
    }
    
    public static BatchScrapingStatusResponse notStarted() {
        return new BatchScrapingStatusResponse(
            null, BatchStatus.NOT_STARTED, 0, 0, 0, 
            null, null, null, 0, 0.0
        );
    }
}
