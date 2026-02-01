package com.str.platform.analysis.infrastructure.persistence.mapper;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.entity.AnalysisResultEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalysisResultEntityMapper")
class AnalysisResultEntityMapperTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final BigDecimal BUDGET = new BigDecimal("250000.00");
    private static final BigDecimal MONTHLY_REVENUE = new BigDecimal("2000.00");
    private static final BigDecimal AVERAGE_DAILY_RATE = new BigDecimal("120.00");
    private static final double ANNUAL_ROI = 7.5;
    private static final int PAYBACK_PERIOD = 60;
    private static final BigDecimal OCCUPANCY_RATE = new BigDecimal("0.68");

    private AnalysisResultEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AnalysisResultEntityMapper();
    }

    @Nested
    @DisplayName("Entity to Domain Mapping")
    class EntityToDomainMapping {

        @Test
        void shouldMapCompleteEntityToDomain() {
            var entity = createCompleteAnalysisResultEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain).isNotNull();
            assertThat(domain.getId()).isEqualTo(entity.getId());
            assertThat(domain.getConfiguration().getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(domain.getConfiguration().getBudget().getAmount()).isEqualByComparingTo(BUDGET);
            assertThat(domain.getDataQuality()).isEqualTo(AnalysisResult.DataQuality.HIGH);
        }

        @Test
        void shouldMapInvestmentMetrics() {
            var entity = createCompleteAnalysisResultEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getMetrics()).isNotNull();
            assertThat(domain.getMetrics().getMonthlyRevenueConservative().getAmount()).isEqualByComparingTo(MONTHLY_REVENUE);
            assertThat(domain.getMetrics().getAnnualROI()).isEqualTo(ANNUAL_ROI);
            assertThat(domain.getMetrics().getPaybackPeriodMonths()).isEqualTo(PAYBACK_PERIOD);
            assertThat(domain.getMetrics().getOccupancyRate()).isEqualTo(OCCUPANCY_RATE.doubleValue());
        }

        @Test
        void shouldMapMarketAnalysisWithSeasonalityConversion() {
            var entity = createCompleteAnalysisResultEntity();

            var domain = mapper.toDomain(entity);

            assertThat(domain.getMarketAnalysis()).isNotNull();
            assertThat(domain.getMarketAnalysis().getTotalListings()).isEqualTo(200);
            assertThat(domain.getMarketAnalysis().getAverageDailyRate().getAmount()).isEqualByComparingTo(AVERAGE_DAILY_RATE);
            assertThat(domain.getMarketAnalysis().getSeasonalityIndex()).isEqualTo(0.42);
            assertThat(domain.getMarketAnalysis().getGrowthTrend()).isEqualTo(MarketAnalysis.GrowthTrend.INCREASING);
            assertThat(domain.getMarketAnalysis().getCompetitionDensity()).isEqualTo(MarketAnalysis.CompetitionDensity.HIGH);
        }

        @Test
        void shouldMapInvestmentTypeCorrectly() {
            var buyEntity = createCompleteAnalysisResultEntity();
            buyEntity.setInvestmentType(AnalysisResultEntity.InvestmentType.BUY);

            var rentEntity = createCompleteAnalysisResultEntity();
            rentEntity.setInvestmentType(AnalysisResultEntity.InvestmentType.RENT);
            rentEntity.setBudget(new BigDecimal("20000.00"));

            assertThat(mapper.toDomain(buyEntity).getConfiguration().getInvestmentType())
                .isEqualTo(InvestmentConfiguration.InvestmentType.BUY);
            assertThat(mapper.toDomain(rentEntity).getConfiguration().getInvestmentType())
                .isEqualTo(InvestmentConfiguration.InvestmentType.RENT);
        }

        @Test
        void shouldRestoreCachedState() {
            var entity = createCompleteAnalysisResultEntity();
            entity.setCached(true);

            var domain = mapper.toDomain(entity);

            assertThat(domain.isCached()).isTrue();
        }

        @Test
        void shouldReturnNullForNullEntity() {
            var domain = mapper.toDomain(null);

            assertThat(domain).isNull();
        }
    }

    @Nested
    @DisplayName("Domain to Entity Mapping")
    class DomainToEntityMapping {

        @Test
        void shouldMapCompleteDomainToEntity() {
            var domain = createCompleteAnalysisResult();

            var entity = mapper.toEntity(domain, LOCATION_ID);

            assertThat(entity).isNotNull();
            assertThat(entity.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(entity.getBudget()).isEqualByComparingTo(BUDGET);
            assertThat(entity.getCurrency()).isEqualTo("EUR");
            assertThat(entity.getPropertyType()).isEqualTo("APARTMENT");
            assertThat(entity.getDataQuality()).isEqualTo(AnalysisResultEntity.DataQuality.HIGH);
        }

        @Test
        void shouldMapMetricsToEmbeddedData() {
            var domain = createCompleteAnalysisResult();

            var entity = mapper.toEntity(domain, LOCATION_ID);

            assertThat(entity.getMetrics()).isNotNull();
            assertThat(entity.getMetrics().getMonthlyRevenueConservative()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(entity.getMetrics().getMonthlyRevenueExpected()).isEqualByComparingTo(MONTHLY_REVENUE);
            assertThat(entity.getMetrics().getMonthlyRevenueOptimistic()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(entity.getMetrics().getAnnualRoiExpected()).isEqualByComparingTo(BigDecimal.valueOf(ANNUAL_ROI));
        }

        @Test
        void shouldMapMarketAnalysisWithSeasonalityConversion() {
            var domain = createCompleteAnalysisResult();

            var entity = mapper.toEntity(domain, LOCATION_ID);

            assertThat(entity.getMarketAnalysis()).isNotNull();
            assertThat(entity.getMarketAnalysis().getTotalListings()).isEqualTo(180);
            assertThat(entity.getMarketAnalysis().getSeasonality()).isEqualTo("0.38");
            assertThat(entity.getMarketAnalysis().getGrowthTrend()).isEqualTo("STABLE");
            assertThat(entity.getMarketAnalysis().getCompetitionDensity()).isEqualTo("MEDIUM");
        }

        @Test
        void shouldIncludeAverageDailyRateInMetrics() {
            var domain = createCompleteAnalysisResult();

            var entity = mapper.toEntity(domain, LOCATION_ID);

            assertThat(entity.getMetrics().getAverageDailyRate()).isEqualByComparingTo(AVERAGE_DAILY_RATE);
        }

        @Test
        void shouldMapCachedState() {
            var domain = createCompleteAnalysisResult();
            domain.markAsCached();

            var entity = mapper.toEntity(domain, LOCATION_ID);

            assertThat(entity.getCached()).isTrue();
        }

        @Test
        void shouldReturnNullForNullDomain() {
            var entity = mapper.toEntity(null, LOCATION_ID);

            assertThat(entity).isNull();
        }
    }

    @Nested
    @DisplayName("Enum Mapping")
    class EnumMapping {

        @Test
        void shouldMapAllDataQualityLevels() {
            assertDataQualityMapping(AnalysisResult.DataQuality.HIGH, AnalysisResultEntity.DataQuality.HIGH);
            assertDataQualityMapping(AnalysisResult.DataQuality.MEDIUM, AnalysisResultEntity.DataQuality.MEDIUM);
            assertDataQualityMapping(AnalysisResult.DataQuality.LOW, AnalysisResultEntity.DataQuality.LOW);
        }

        private void assertDataQualityMapping(AnalysisResult.DataQuality domainQuality, AnalysisResultEntity.DataQuality entityQuality) {
            var domain = createAnalysisResultWithDataQuality(domainQuality);
            var entity = mapper.toEntity(domain, LOCATION_ID);
            assertThat(entity.getDataQuality()).isEqualTo(entityQuality);

            var entityWithQuality = createAnalysisResultEntityWithDataQuality(entityQuality);
            var mappedDomain = mapper.toDomain(entityWithQuality);
            assertThat(mappedDomain.getDataQuality()).isEqualTo(domainQuality);
        }
    }

    private AnalysisResultEntity createCompleteAnalysisResultEntity() {
        var metricsData = new AnalysisResultEntity.MetricsData();
        metricsData.setMonthlyRevenueConservative(MONTHLY_REVENUE);
        metricsData.setMonthlyRevenueExpected(MONTHLY_REVENUE);
        metricsData.setMonthlyRevenueOptimistic(MONTHLY_REVENUE);
        metricsData.setAnnualRoiConservative(BigDecimal.valueOf(ANNUAL_ROI));
        metricsData.setAnnualRoiExpected(BigDecimal.valueOf(ANNUAL_ROI));
        metricsData.setAnnualRoiOptimistic(BigDecimal.valueOf(ANNUAL_ROI));
        metricsData.setPaybackPeriodMonths(PAYBACK_PERIOD);
        metricsData.setOccupancyRate(OCCUPANCY_RATE);
        metricsData.setAverageDailyRate(AVERAGE_DAILY_RATE);

        var marketData = new AnalysisResultEntity.MarketAnalysisData();
        marketData.setTotalListings(200);
        marketData.setAverageDailyRate(AVERAGE_DAILY_RATE);
        marketData.setOccupancyRate(OCCUPANCY_RATE);
        marketData.setEstimatedMonthlyRevenue(new BigDecimal("2448.00"));
        marketData.setSeasonality("0.42");
        marketData.setGrowthTrend("INCREASING");
        marketData.setCompetitionDensity("HIGH");
        marketData.setAdditionalMetrics(new HashMap<>());

        return AnalysisResultEntity.builder()
            .id(UUID.randomUUID())
            .locationId(LOCATION_ID)
            .investmentType(AnalysisResultEntity.InvestmentType.BUY)
            .budget(BUDGET)
            .currency("EUR")
            .propertyType("APARTMENT")
            .metrics(metricsData)
            .marketAnalysis(marketData)
            .dataQuality(AnalysisResultEntity.DataQuality.HIGH)
            .cached(false)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private AnalysisResultEntity createAnalysisResultEntityWithDataQuality(AnalysisResultEntity.DataQuality quality) {
        var entity = createCompleteAnalysisResultEntity();
        entity.setDataQuality(quality);
        return entity;
    }

    private AnalysisResult createCompleteAnalysisResult() {
        var config = new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            new Money(BUDGET, Money.Currency.EUR),
            InvestmentConfiguration.PropertyType.APARTMENT,
            null
        );

        var metrics = new InvestmentMetrics(
            new Money(new BigDecimal("1500.00"), Money.Currency.EUR),
            new Money(MONTHLY_REVENUE, Money.Currency.EUR),
            new Money(new BigDecimal("2500.00"), Money.Currency.EUR),
            ANNUAL_ROI,
            PAYBACK_PERIOD,
            OCCUPANCY_RATE.doubleValue()
        );

        var marketAnalysis = new MarketAnalysis(
            180,
            new Money(AVERAGE_DAILY_RATE, Money.Currency.EUR),
            OCCUPANCY_RATE,
            new Money(new BigDecimal("2448.00"), Money.Currency.EUR),
            0.38,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );

        return new AnalysisResult(config, metrics, marketAnalysis, AnalysisResult.DataQuality.HIGH);
    }

    private AnalysisResult createAnalysisResultWithDataQuality(AnalysisResult.DataQuality quality) {
        var result = createCompleteAnalysisResult();
        return new AnalysisResult(
            result.getConfiguration(),
            result.getMetrics(),
            result.getMarketAnalysis(),
            quality
        );
    }
}
