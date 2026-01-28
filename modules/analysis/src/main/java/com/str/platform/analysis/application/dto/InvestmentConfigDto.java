package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Investment configuration DTO
 */
@Schema(description = "Investment configuration details")
public record InvestmentConfigDto(
    @Schema(description = "Investment type", example = "BUY")
    String investmentType,
    
    @Schema(description = "Budget amount", example = "150000")
    BigDecimal budget,
    
    @Schema(description = "Budget currency", example = "EUR")
    String currency,
    
    @Schema(description = "Property type", example = "APARTMENT")
    String propertyType,
    
    @Schema(description = "Investment goal", example = "MAX_ROI")
    String goal,
    
    @Schema(description = "Accepts renovation", example = "false")
    boolean acceptsRenovation
) {}
