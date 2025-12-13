package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for investment analysis
 */
@Schema(description = "Investment analysis request")
public record AnalysisRequest(
    
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Schema(description = "Latitude coordinate", example = "48.8566")
    Double latitude,
    
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Schema(description = "Longitude coordinate", example = "2.3522")
    Double longitude,
    
    @NotNull(message = "Investment type is required")
    @Schema(description = "Type of investment", example = "BUY", 
            allowableValues = {"BUY", "RENT"})
    String investmentType,
    
    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    @Schema(description = "Investment budget in EUR", example = "150000")
    BigDecimal budget,
    
    @NotNull(message = "Property type is required")
    @Schema(description = "Type of property", example = "APARTMENT",
            allowableValues = {"APARTMENT", "HOUSE", "ROOM"})
    String propertyType,
    
    @NotNull(message = "Investment goal is required")
    @Schema(description = "Primary investment goal", example = "MAX_ROI",
            allowableValues = {"MAX_ROI", "STABLE_INCOME", "QUICK_PAYBACK"})
    String investmentGoal,
    
    @Schema(description = "Accepts properties needing renovation", example = "false")
    Boolean acceptsRenovation
) {
    public AnalysisRequest {
        // Set default for acceptsRenovation if null
        if (acceptsRenovation == null) {
            acceptsRenovation = false;
        }
    }
}
