package com.str.platform.analysis.application.mapper;

import com.str.platform.analysis.application.dto.*;
import com.str.platform.analysis.domain.model.AnalysisResult;
import com.str.platform.analysis.domain.model.Money;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting domain models to DTOs for API responses
 */
@Component
public class AnalysisResponseMapper {
    
    /**
     * Map AnalysisResult domain model to AnalysisResponse DTO
     */
    public AnalysisResponse toResponse(AnalysisResult result) {
        return new AnalysisResponse(
            result.getId().toString(),
            result.getConfiguration().getLocationId().toString(),
            toInvestmentConfigDto(result),
            toInvestmentMetricsDto(result),
            toMarketAnalysisDto(result),
            result.getDataQuality().name(),
            calculateMarketScore(result),
            calculateConfidence(result),
            result.isCached(),
            result.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }
    
    /**
     * Map investment configuration to InvestmentConfigDto
     */
    private InvestmentConfigDto toInvestmentConfigDto(AnalysisResult result) {
        return new InvestmentConfigDto(
            result.getConfiguration().getInvestmentType().name(),
            result.getConfiguration().getBudget().getAmount(),
            result.getConfiguration().getBudget().getCurrency().name(),
            result.getConfiguration().getPropertyType().name(),
            result.getConfiguration().getGoal() != null ? result.getConfiguration().getGoal().name() : null,
            result.getConfiguration().isAcceptsRenovation()
        );
    }
    
    /**
     * Map investment metrics to InvestmentMetricsDto
     */
    private InvestmentMetricsDto toInvestmentMetricsDto(AnalysisResult result) {
        return new InvestmentMetricsDto(
            toMoneyDto(result.getMetrics().getMonthlyRevenueConservative()),
            toMoneyDto(result.getMetrics().getMonthlyRevenueExpected()),
            toMoneyDto(result.getMetrics().getMonthlyRevenueOptimistic()),
            result.getMetrics().getAnnualROI(),
            result.getMetrics().getPaybackPeriodMonths(),
            result.getMetrics().getOccupancyRate(),
            toMoneyDto(result.getMetrics().getAnnualRevenue()),
            result.getMetrics().isViableInvestment()
        );
    }
    
    /**
     * Map market analysis to MarketAnalysisDto
     */
    private MarketAnalysisDto toMarketAnalysisDto(AnalysisResult result) {
        return new MarketAnalysisDto(
            result.getMarketAnalysis().getTotalListings(),
            toMoneyDto(result.getMarketAnalysis().getAverageDailyRate()),
            result.getMarketAnalysis().getAverageOccupancyRate(),
            toMoneyDto(result.getMarketAnalysis().getEstimatedMonthlyRevenue()),
            result.getMarketAnalysis().getSeasonalityIndex(),
            result.getMarketAnalysis().getGrowthTrend().name(),
            result.getMarketAnalysis().getCompetitionDensity().name()
        );
    }
    
    /**
     * Map Money domain object to MoneyDto
     */
    private MoneyDto toMoneyDto(Money money) {
        return new MoneyDto(
            money.getAmount(),
            money.getCurrency().name()
        );
    }

    private int calculateMarketScore(AnalysisResult result) {
        int score = switch (result.getDataQuality()) {
            case HIGH -> 75;
            case MEDIUM -> 60;
            case LOW -> 45;
        };

        score += switch (result.getMarketAnalysis().getGrowthTrend()) {
            case INCREASING -> 15;
            case STABLE -> 8;
            case DECLINING -> -12;
        };

        score += switch (result.getMarketAnalysis().getCompetitionDensity()) {
            case LOW -> 8;
            case MEDIUM -> 0;
            case HIGH -> -8;
        };

        double occupancyPct = result.getMarketAnalysis().getAverageOccupancyRate().doubleValue() * 100.0;
        if (occupancyPct >= 80) {
            score += 7;
        } else if (occupancyPct >= 65) {
            score += 3;
        } else if (occupancyPct < 50) {
            score -= 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String calculateConfidence(AnalysisResult result) {
        double roi = result.getMetrics().getAnnualROI();

        if (roi > 8.0) {
            return "HIGH";
        }

        if (roi > 5.0) {
            return "MEDIUM";
        }

        return "LOW";
    }
}
