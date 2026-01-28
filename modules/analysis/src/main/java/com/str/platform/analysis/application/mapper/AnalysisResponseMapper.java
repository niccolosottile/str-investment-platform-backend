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
            toLocationDto(result),
            toInvestmentConfigDto(result),
            toInvestmentMetricsDto(result),
            toMarketAnalysisDto(result),
            result.getDataQuality().name(),
            result.isCached(),
            result.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }
    
    /**
     * Map location coordinates to LocationDto
     */
    private LocationDto toLocationDto(AnalysisResult result) {
        return new LocationDto(
            result.getConfiguration().getLocation().getLatitude(),
            result.getConfiguration().getLocation().getLongitude()
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
            result.getConfiguration().getGoal().name(),
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
}
