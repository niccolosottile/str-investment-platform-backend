package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Location coordinates DTO
 */
@Schema(description = "Geographic location coordinates")
public record LocationDto(
    @Schema(description = "Latitude", example = "48.8566")
    double latitude,
    
    @Schema(description = "Longitude", example = "2.3522")
    double longitude
) {}
