package com.str.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for driving time calculation requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for calculating driving time between two points")
public class DrivingTimeRequest {

    @NotNull(message = "Origin latitude is required")
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    @Schema(description = "Origin latitude", example = "41.9028")
    private Double originLatitude;

    @NotNull(message = "Origin longitude is required")
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    @Schema(description = "Origin longitude", example = "12.4964")
    private Double originLongitude;

    @NotNull(message = "Destination latitude is required")
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    @Schema(description = "Destination latitude", example = "45.4642")
    private Double destinationLatitude;

    @NotNull(message = "Destination longitude is required")
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    @Schema(description = "Destination longitude", example = "9.1900")
    private Double destinationLongitude;
}
