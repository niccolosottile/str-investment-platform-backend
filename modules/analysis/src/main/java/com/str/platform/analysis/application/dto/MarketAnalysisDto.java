package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Market analysis DTO
 */
@Schema(description = "Market analysis data including competition and trends")
public record MarketAnalysisDto(
    @Schema(description = "Total listings in area", example = "127")
    int totalListings,
    
    @Schema(description = "Average daily rate")
    MoneyDto averageDailyRate,
    
    @Schema(description = "Average occupancy rate", example = "0.65")
    BigDecimal averageOccupancyRate,
    
    @Schema(description = "Estimated monthly revenue")
    MoneyDto estimatedMonthlyRevenue,
    
    @Schema(description = "Seasonality index", example = "1.0")
    double seasonalityIndex,
    
    @Schema(description = "Market growth trend", example = "INCREASING")
    String growthTrend,
    
    @Schema(description = "Competition density", example = "MEDIUM")
    String competitionDensity
) {}
