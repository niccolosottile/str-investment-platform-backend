package com.str.platform.location.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for location search requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for searching locations")
public class LocationSearchRequest {

    @NotNull(message = "Query is required")
    @Schema(description = "Search query (city, region, or country)", example = "Rome, Italy")
    private String query;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 10, message = "Limit cannot exceed 10")
    @Schema(description = "Maximum number of results to return", example = "5", defaultValue = "5")
    private Integer limit;
}
