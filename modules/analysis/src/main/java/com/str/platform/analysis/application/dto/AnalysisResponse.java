package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for investment analysis results
 */
@Schema(description = "Investment analysis results")
public record AnalysisResponse(
    
    @Schema(description = "Unique analysis ID")
    String id,
    
    @Schema(description = "Location ID")
    String locationId,
    
    @Schema(description = "Investment configuration")
    InvestmentConfigDto configuration,
    
    @Schema(description = "Investment metrics")
    InvestmentMetricsDto metrics,
    
    @Schema(description = "Market analysis")
    MarketAnalysisDto marketAnalysis,
    
    @Schema(description = "Data quality indicator", example = "HIGH")
    String dataQuality,

    @Schema(description = "Normalized market score (0-100)", example = "82")
    int marketScore,

    @Schema(description = "Confidence level for the investment outcome", example = "HIGH")
    String confidence,
    
    @Schema(description = "Whether data is from cache")
    boolean cached,
    
    @Schema(description = "Analysis creation timestamp")
    Instant createdAt
) {}
