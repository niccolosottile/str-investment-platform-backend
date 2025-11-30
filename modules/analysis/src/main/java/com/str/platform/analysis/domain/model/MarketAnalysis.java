package com.str.platform.analysis.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Value object containing market analysis data.
 */
@Getter
@AllArgsConstructor
public class MarketAnalysis {
    
    private final int totalListings;
    private final Money averageDailyRate;
    private final double seasonalityIndex;
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
