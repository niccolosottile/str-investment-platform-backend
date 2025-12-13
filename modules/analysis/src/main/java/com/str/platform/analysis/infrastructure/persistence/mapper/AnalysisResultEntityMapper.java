package com.str.platform.analysis.infrastructure.persistence.mapper;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.entity.AnalysisResultEntity;
import com.str.platform.location.domain.model.Coordinates;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

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

        // Create investment configuration with proper types
        InvestmentConfiguration.PropertyType propertyType = InvestmentConfiguration.PropertyType.valueOf(entity.getPropertyType());
        
        // Note: We don't have actual Coordinates stored, so we create a placeholder
        // In real implementation, this should be fetched from the Location service
        Coordinates location = new Coordinates(0.0, 0.0); // Placeholder
        
        InvestmentConfiguration config = new InvestmentConfiguration(
            location,
            mapInvestmentTypeToDomain(entity.getInvestmentType()),
            new Money(entity.getBudget(), Money.Currency.valueOf(entity.getCurrency())),
            propertyType,
            null // goals not stored in entity for now
        );

        AnalysisResult result = new AnalysisResult(
            config,
            metrics,
            marketAnalysis,
            mapDataQualityToDomain(entity.getDataQuality())
        );

        // Set ID using reflection since BaseEntity doesn't expose setter
        try {
            java.lang.reflect.Field idField = com.str.platform.shared.domain.common.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(result, entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set analysis result ID", e);
        }

        // Set cache metadata
        if (entity.getCached()) {
            result.markAsCached();
        }

        return result;
    }

    /**
     * Convert domain model to JPA entity
     * @param domain The domain model to convert
     * @param locationId The UUID of the location entity (must be found/created before calling this method)
     */
    public AnalysisResultEntity toEntity(AnalysisResult domain, UUID locationId) {
        if (domain == null) {
            return null;
        }

        return AnalysisResultEntity.builder()
            .id(domain.getId())
            .locationId(locationId)
            .investmentType(mapInvestmentTypeToEntity(domain.getConfiguration().getInvestmentType()))
            .budget(domain.getConfiguration().getBudget().getAmount())
            .currency(domain.getConfiguration().getBudget().getCurrency().name())
            .propertyType(domain.getConfiguration().getPropertyType().name())
            .metrics(mapMetricsToEntity(domain.getMetrics()))
            .marketAnalysis(mapMarketAnalysisToEntity(domain.getMarketAnalysis()))
            .dataQuality(mapDataQualityToEntity(domain.getDataQuality()))
            .cached(domain.isCached())
            .cacheExpiresAt(null) // Not tracked in domain model
            .build();
    }

    private InvestmentMetrics mapMetricsToDomain(AnalysisResultEntity.MetricsData data) {
        return new InvestmentMetrics(
            new Money(data.getMonthlyRevenueConservative(), Money.Currency.EUR),
            new Money(data.getMonthlyRevenueExpected(), Money.Currency.EUR),
            new Money(data.getMonthlyRevenueOptimistic(), Money.Currency.EUR),
            data.getAnnualRoiExpected().doubleValue(),
            data.getPaybackPeriodMonths(),
            data.getOccupancyRate().doubleValue()
        );
    }

    private AnalysisResultEntity.MetricsData mapMetricsToEntity(InvestmentMetrics domain) {
        AnalysisResultEntity.MetricsData data = new AnalysisResultEntity.MetricsData();
        data.setMonthlyRevenueConservative(domain.getMonthlyRevenueConservative().getAmount());
        data.setMonthlyRevenueExpected(domain.getMonthlyRevenueExpected().getAmount());
        data.setMonthlyRevenueOptimistic(domain.getMonthlyRevenueOptimistic().getAmount());
        data.setAnnualRoiConservative(BigDecimal.valueOf(domain.getAnnualROI()));
        data.setAnnualRoiExpected(BigDecimal.valueOf(domain.getAnnualROI()));
        data.setAnnualRoiOptimistic(BigDecimal.valueOf(domain.getAnnualROI()));
        data.setPaybackPeriodMonths(domain.getPaybackPeriodMonths());
        data.setOccupancyRate(BigDecimal.valueOf(domain.getOccupancyRate()));
        data.setAverageDailyRate(null); // Not available in domain model
        return data;
    }

    private MarketAnalysis mapMarketAnalysisToDomain(AnalysisResultEntity.MarketAnalysisData data) {
        return new MarketAnalysis(
            data.getTotalListings(),
            new Money(data.getAverageDailyRate(), Money.Currency.EUR),
            Double.parseDouble(data.getSeasonality()), // Convert String to double
            MarketAnalysis.GrowthTrend.valueOf(data.getGrowthTrend()),
            MarketAnalysis.CompetitionDensity.valueOf(data.getCompetitionDensity())
        );
    }

    private AnalysisResultEntity.MarketAnalysisData mapMarketAnalysisToEntity(MarketAnalysis domain) {
        AnalysisResultEntity.MarketAnalysisData data = new AnalysisResultEntity.MarketAnalysisData();
        data.setTotalListings(domain.getTotalListings());
        data.setAverageDailyRate(domain.getAverageDailyRate().getAmount());
        data.setSeasonality(String.valueOf(domain.getSeasonalityIndex())); // Convert double to String
        data.setGrowthTrend(domain.getGrowthTrend().name());
        data.setCompetitionDensity(domain.getCompetitionDensity().name()); // Convert enum to String
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
