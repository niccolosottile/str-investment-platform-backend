package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for investment analysis results
 */
@Schema(description = "Investment analysis results")
public record AnalysisResponse(
    
    @Schema(description = "Unique analysis ID")
    String id,
    
    @Schema(description = "Analysis location")
    LocationDto location,
    
    @Schema(description = "Investment configuration")
    InvestmentConfigDto configuration,
    
    @Schema(description = "Investment metrics")
    InvestmentMetricsDto metrics,
    
    @Schema(description = "Market analysis")
    MarketAnalysisDto marketAnalysis,
    
    @Schema(description = "Data quality indicator", example = "HIGH")
    String dataQuality,
    
    @Schema(description = "Whether data is from cache")
    boolean cached,
    
    @Schema(description = "Analysis creation timestamp")
    Instant createdAt
) {
    
    public record LocationDto(
        @Schema(description = "Latitude", example = "48.8566")
        double latitude,
        
        @Schema(description = "Longitude", example = "2.3522")
        double longitude
    ) {}
    
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
    
    public record InvestmentMetricsDto(
        @Schema(description = "Monthly revenue (conservative scenario)")
        MoneyDto monthlyRevenueConservative,
        
        @Schema(description = "Monthly revenue (expected scenario)")
        MoneyDto monthlyRevenueExpected,
        
        @Schema(description = "Monthly revenue (optimistic scenario)")
        MoneyDto monthlyRevenueOptimistic,
        
        @Schema(description = "Annual ROI percentage", example = "8.5")
        double annualROI,
        
        @Schema(description = "Payback period in months", example = "84")
        int paybackPeriodMonths,
        
        @Schema(description = "Expected occupancy rate", example = "0.75")
        double occupancyRate,
        
        @Schema(description = "Annual revenue based on expected scenario")
        MoneyDto annualRevenue,
        
        @Schema(description = "Whether this is a viable investment")
        boolean viableInvestment
    ) {}
    
    public record MarketAnalysisDto(
        @Schema(description = "Total listings in area", example = "127")
        int totalListings,
        
        @Schema(description = "Average daily rate")
        MoneyDto averageDailyRate,
        
        @Schema(description = "Seasonality index", example = "1.0")
        double seasonalityIndex,
        
        @Schema(description = "Market growth trend", example = "INCREASING")
        String growthTrend,
        
        @Schema(description = "Competition density", example = "MEDIUM")
        String competitionDensity
    ) {}
    
    public record MoneyDto(
        @Schema(description = "Amount", example = "1250.50")
        BigDecimal amount,
        
        @Schema(description = "Currency", example = "EUR")
        String currency
    ) {}
}
