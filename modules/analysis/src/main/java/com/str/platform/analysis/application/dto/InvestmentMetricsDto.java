package com.str.platform.analysis.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Investment metrics DTO
 */
@Schema(description = "Calculated investment performance metrics")
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
