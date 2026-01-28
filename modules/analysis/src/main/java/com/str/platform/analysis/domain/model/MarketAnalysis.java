package com.str.platform.analysis.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Value object containing comprehensive market analysis data based on scraped property information.
 * Includes pricing, occupancy, competition, and market trend indicators.
 */
@Getter
@AllArgsConstructor
public class MarketAnalysis {
    
    private final int totalListings;
    private final Money averageDailyRate;
    private final BigDecimal averageOccupancyRate;  // 0.0-1.0 (e.g., 0.65 = 65%)
    private final Money estimatedMonthlyRevenue;    // ADR × 30 × occupancy
    private final double seasonalityIndex;           // Variation: (max-min)/min
    private final GrowthTrend growthTrend;
    private final CompetitionDensity competitionDensity;
    
    public enum GrowthTrend {
        INCREASING,
        STABLE,
        DECLINING
    }
    
    public enum CompetitionDensity {
        LOW,     // <10 properties per km²
        MEDIUM,  // 10-30 properties per km²
        HIGH     // >30 properties per km²
    }
}
