package com.str.platform.scraping.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for starting a batch scraping operation.
 */
@Schema(description = "Request to start batch scraping for multiple locations")
public record BatchScrapingRequest(
    
    @Schema(description = "Batch processing strategy", 
            example = "STALE_ONLY",
            allowableValues = {"ALL_LOCATIONS", "STALE_ONLY"})
    BatchStrategy strategy,
    
    @Schema(description = "Delay between locations in minutes", 
            example = "10",
            defaultValue = "10")
    Integer delayMinutes,
    
    @Schema(description = "Number of days threshold for stale data (used with STALE_ONLY strategy)", 
            example = "30",
            defaultValue = "30")
    Integer staleThresholdDays
) {
    
    @JsonCreator
    public BatchScrapingRequest(
        @JsonProperty("strategy") BatchStrategy strategy,
        @JsonProperty("delayMinutes") Integer delayMinutes,
        @JsonProperty("staleThresholdDays") Integer staleThresholdDays
    ) {
        this.strategy = strategy != null ? strategy : BatchStrategy.STALE_ONLY;
        this.delayMinutes = delayMinutes != null ? delayMinutes : 10;
        this.staleThresholdDays = staleThresholdDays != null ? staleThresholdDays : 30;
    }
    
    public enum BatchStrategy {
        ALL_LOCATIONS,
        STALE_ONLY
    }
}
