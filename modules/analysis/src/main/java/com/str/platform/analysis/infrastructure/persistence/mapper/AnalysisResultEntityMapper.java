package com.str.platform.analysis.infrastructure.persistence.mapper;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.entity.AnalysisResultEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;

/**
 * Mapper for converting between AnalysisResult domain model and AnalysisResultEntity.
 */
@Component
public class AnalysisResultEntityMapper {

    /**
     * Convert JPA entity to domain model
     */
    public AnalysisResult toDomain(AnalysisResultEntity entity) {
        if (entity == null) {
            return null;
        }

        // Convert metrics
        InvestmentMetrics metrics = mapMetricsToDomain(entity.getMetrics());

        // Convert market analysis
        MarketAnalysis marketAnalysis = mapMarketAnalysisToDomain(entity.getMarketAnalysis());

        // Create investment configuration
        InvestmentConfiguration config = new InvestmentConfiguration(
            entity.getLocationId(),
            mapInvestmentTypeToDomain(entity.getInvestmentType()),
            new Money(entity.getBudget(), entity.getCurrency()),
            entity.getPropertyType(),
            null // goals not stored in entity for now
        );

        AnalysisResult result = new AnalysisResult(
            entity.getId(),
            config,
            metrics,
            marketAnalysis,
            mapDataQualityToDomain(entity.getDataQuality())
        );

        // Set cache metadata
        if (entity.getCached()) {
            result.markAsCached(entity.getCacheExpiresAt());
        }

        return result;
    }

    /**
     * Convert domain model to JPA entity
     */
    public AnalysisResultEntity toEntity(AnalysisResult domain) {
        if (domain == null) {
            return null;
        }

        return AnalysisResultEntity.builder()
            .id(domain.getId())
            .locationId(domain.getConfiguration().locationId())
            .investmentType(mapInvestmentTypeToEntity(domain.getConfiguration().type()))
            .budget(domain.getConfiguration().budget().amount())
            .currency(domain.getConfiguration().budget().currency())
            .propertyType(domain.getConfiguration().propertyType())
            .metrics(mapMetricsToEntity(domain.getMetrics()))
            .marketAnalysis(mapMarketAnalysisToEntity(domain.getMarketAnalysis()))
            .dataQuality(mapDataQualityToEntity(domain.getDataQuality()))
            .cached(domain.isCached())
            .cacheExpiresAt(domain.getCacheExpiresAt())
            .build();
    }

    private InvestmentMetrics mapMetricsToDomain(AnalysisResultEntity.MetricsData data) {
        return new InvestmentMetrics(
            new Money(data.getMonthlyRevenueConservative(), "EUR"),
            new Money(data.getMonthlyRevenueExpected(), "EUR"),
            new Money(data.getMonthlyRevenueOptimistic(), "EUR"),
            data.getAnnualRoiConservative().doubleValue(),
            data.getAnnualRoiExpected().doubleValue(),
            data.getAnnualRoiOptimistic().doubleValue(),
            data.getPaybackPeriodMonths(),
            data.getOccupancyRate().doubleValue(),
            new Money(data.getAverageDailyRate(), "EUR")
        );
    }

    private AnalysisResultEntity.MetricsData mapMetricsToEntity(InvestmentMetrics domain) {
        AnalysisResultEntity.MetricsData data = new AnalysisResultEntity.MetricsData();
        data.setMonthlyRevenueConservative(domain.monthlyRevenueConservative().amount());
        data.setMonthlyRevenueExpected(domain.monthlyRevenueExpected().amount());
        data.setMonthlyRevenueOptimistic(domain.monthlyRevenueOptimistic().amount());
        data.setAnnualRoiConservative(BigDecimal.valueOf(domain.annualRoiConservative()));
        data.setAnnualRoiExpected(BigDecimal.valueOf(domain.annualRoiExpected()));
        data.setAnnualRoiOptimistic(BigDecimal.valueOf(domain.annualRoiOptimistic()));
        data.setPaybackPeriodMonths(domain.paybackPeriodMonths());
        data.setOccupancyRate(BigDecimal.valueOf(domain.occupancyRate()));
        data.setAverageDailyRate(domain.averageDailyRate().amount());
        return data;
    }

    private MarketAnalysis mapMarketAnalysisToDomain(AnalysisResultEntity.MarketAnalysisData data) {
        return new MarketAnalysis(
            data.getTotalListings(),
            new Money(data.getAverageDailyRate(), "EUR"),
            data.getSeasonality(),
            MarketAnalysis.GrowthTrend.valueOf(data.getGrowthTrend()),
            data.getCompetitionDensity()
        );
    }

    private AnalysisResultEntity.MarketAnalysisData mapMarketAnalysisToEntity(MarketAnalysis domain) {
        AnalysisResultEntity.MarketAnalysisData data = new AnalysisResultEntity.MarketAnalysisData();
        data.setTotalListings(domain.totalListings());
        data.setAverageDailyRate(domain.averageDailyRate().amount());
        data.setSeasonality(domain.seasonality());
        data.setGrowthTrend(domain.growthTrend().name());
        data.setCompetitionDensity(domain.competitionDensity());
        data.setAdditionalMetrics(new HashMap<>());
        return data;
    }

    private InvestmentConfiguration.InvestmentType mapInvestmentTypeToDomain(AnalysisResultEntity.InvestmentType entityType) {
        return switch (entityType) {
            case BUY -> InvestmentConfiguration.InvestmentType.BUY;
            case RENT -> InvestmentConfiguration.InvestmentType.RENT;
        };
    }

    private AnalysisResultEntity.InvestmentType mapInvestmentTypeToEntity(InvestmentConfiguration.InvestmentType domainType) {
        return switch (domainType) {
            case BUY -> AnalysisResultEntity.InvestmentType.BUY;
            case RENT -> AnalysisResultEntity.InvestmentType.RENT;
        };
    }

    private AnalysisResult.DataQuality mapDataQualityToDomain(AnalysisResultEntity.DataQuality entityQuality) {
        return switch (entityQuality) {
            case HIGH -> AnalysisResult.DataQuality.HIGH;
            case MEDIUM -> AnalysisResult.DataQuality.MEDIUM;
            case LOW -> AnalysisResult.DataQuality.LOW;
        };
    }

    private AnalysisResultEntity.DataQuality mapDataQualityToEntity(AnalysisResult.DataQuality domainQuality) {
        return switch (domainQuality) {
            case HIGH -> AnalysisResultEntity.DataQuality.HIGH;
            case MEDIUM -> AnalysisResultEntity.DataQuality.MEDIUM;
            case LOW -> AnalysisResultEntity.DataQuality.LOW;
        };
    }
}
