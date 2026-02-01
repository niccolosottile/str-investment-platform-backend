package com.str.platform.analysis.application.mapper;

import com.str.platform.analysis.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalysisResponseMapper")
class AnalysisResponseMapperTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final BigDecimal BUDGET = new BigDecimal("300000.00");
    private static final BigDecimal MONTHLY_REVENUE = new BigDecimal("2500.00");
    private static final BigDecimal AVERAGE_DAILY_RATE = new BigDecimal("150.00");
    private static final double ANNUAL_ROI = 8.5;
    private static final int PAYBACK_PERIOD = 48;
    private static final double OCCUPANCY_RATE = 0.72;

    private AnalysisResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AnalysisResponseMapper();
    }

    @Nested
    @DisplayName("Complete Analysis Result Mapping")
    class CompleteAnalysisResultMapping {

        @Test
        void shouldMapCompleteAnalysisResultToResponse() {
            var result = createCompleteAnalysisResult();

            var response = mapper.toResponse(result);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(result.getId().toString());
            assertThat(response.locationId()).isEqualTo(LOCATION_ID.toString());
            assertThat(response.dataQuality()).isEqualTo("HIGH");
            assertThat(response.cached()).isTrue();
        }

        @Test
        void shouldMapInvestmentConfiguration() {
            var result = createCompleteAnalysisResult();

            var response = mapper.toResponse(result);

            assertThat(response.configuration()).isNotNull();
            assertThat(response.configuration().investmentType()).isEqualTo("BUY");
            assertThat(response.configuration().budget()).isEqualByComparingTo(BUDGET);
            assertThat(response.configuration().currency()).isEqualTo("EUR");
            assertThat(response.configuration().propertyType()).isEqualTo("APARTMENT");
            assertThat(response.configuration().acceptsRenovation()).isTrue();
        }

        @Test
        void shouldMapInvestmentMetrics() {
            var result = createCompleteAnalysisResult();

            var response = mapper.toResponse(result);

            assertThat(response.metrics()).isNotNull();
            assertThat(response.metrics().monthlyRevenueConservative().amount()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(response.metrics().monthlyRevenueExpected().amount()).isEqualByComparingTo(MONTHLY_REVENUE);
            assertThat(response.metrics().monthlyRevenueOptimistic().amount()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(response.metrics().annualROI()).isEqualTo(ANNUAL_ROI);
            assertThat(response.metrics().paybackPeriodMonths()).isEqualTo(PAYBACK_PERIOD);
            assertThat(response.metrics().occupancyRate()).isEqualTo(OCCUPANCY_RATE);
        }

        @Test
        void shouldMapMarketAnalysis() {
            var result = createCompleteAnalysisResult();

            var response = mapper.toResponse(result);

            assertThat(response.marketAnalysis()).isNotNull();
            assertThat(response.marketAnalysis().totalListings()).isEqualTo(150);
            assertThat(response.marketAnalysis().averageDailyRate().amount()).isEqualByComparingTo(AVERAGE_DAILY_RATE);
            assertThat(response.marketAnalysis().averageOccupancyRate()).isEqualByComparingTo(BigDecimal.valueOf(OCCUPANCY_RATE));
            assertThat(response.marketAnalysis().seasonalityIndex()).isEqualTo(0.35);
            assertThat(response.marketAnalysis().growthTrend()).isEqualTo("INCREASING");
            assertThat(response.marketAnalysis().competitionDensity()).isEqualTo("MEDIUM");
        }

        @Test
        void shouldPreserveMoneyValues() {
            var result = createCompleteAnalysisResult();

            var response = mapper.toResponse(result);

            assertThat(response.metrics().annualRevenue().amount()).isEqualByComparingTo(new BigDecimal("30000.00"));
            assertThat(response.metrics().annualRevenue().currency()).isEqualTo("EUR");
            assertThat(response.marketAnalysis().estimatedMonthlyRevenue().amount()).isEqualByComparingTo(new BigDecimal("3240.00"));
        }
    }

    @Nested
    @DisplayName("Data Quality Mapping")
    class DataQualityMapping {

        @Test
        void shouldMapHighDataQuality() {
            var result = createAnalysisResultWithDataQuality(AnalysisResult.DataQuality.HIGH);

            var response = mapper.toResponse(result);

            assertThat(response.dataQuality()).isEqualTo("HIGH");
        }

        @Test
        void shouldMapMediumDataQuality() {
            var result = createAnalysisResultWithDataQuality(AnalysisResult.DataQuality.MEDIUM);

            var response = mapper.toResponse(result);

            assertThat(response.dataQuality()).isEqualTo("MEDIUM");
        }

        @Test
        void shouldMapLowDataQuality() {
            var result = createAnalysisResultWithDataQuality(AnalysisResult.DataQuality.LOW);

            var response = mapper.toResponse(result);

            assertThat(response.dataQuality()).isEqualTo("LOW");
        }
    }

    @Nested
    @DisplayName("Cache State Mapping")
    class CacheStateMapping {

        @Test
        void shouldMapCachedState() {
            var result = createCompleteAnalysisResult();
            result.markAsCached();

            var response = mapper.toResponse(result);

            assertThat(response.cached()).isTrue();
        }

        @Test
        void shouldMapUncachedState() {
            var result = createAnalysisResultWithDataQuality(AnalysisResult.DataQuality.HIGH);

            var response = mapper.toResponse(result);

            assertThat(response.cached()).isFalse();
        }
    }

    private AnalysisResult createCompleteAnalysisResult() {
        var config = new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            new Money(BUDGET, Money.Currency.EUR),
            InvestmentConfiguration.PropertyType.APARTMENT,
            InvestmentConfiguration.InvestmentGoal.MAX_ROI
        );
        config.setAcceptsRenovation(true);

        var metrics = new InvestmentMetrics(
            new Money(new BigDecimal("2000.00"), Money.Currency.EUR),
            new Money(MONTHLY_REVENUE, Money.Currency.EUR),
            new Money(new BigDecimal("3000.00"), Money.Currency.EUR),
            ANNUAL_ROI,
            PAYBACK_PERIOD,
            OCCUPANCY_RATE
        );

        var marketAnalysis = new MarketAnalysis(
            150,
            new Money(AVERAGE_DAILY_RATE, Money.Currency.EUR),
            BigDecimal.valueOf(OCCUPANCY_RATE),
            new Money(new BigDecimal("3240.00"), Money.Currency.EUR),
            0.35,
            MarketAnalysis.GrowthTrend.INCREASING,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );

        var result = new AnalysisResult(config, metrics, marketAnalysis, AnalysisResult.DataQuality.HIGH);
        result.markAsCached();
        return result;
    }

    private AnalysisResult createAnalysisResultWithDataQuality(AnalysisResult.DataQuality quality) {
        var config = new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            new Money(BUDGET, Money.Currency.EUR),
            InvestmentConfiguration.PropertyType.APARTMENT,
            null
        );

        var metrics = new InvestmentMetrics(
            new Money(MONTHLY_REVENUE, Money.Currency.EUR),
            new Money(MONTHLY_REVENUE, Money.Currency.EUR),
            new Money(MONTHLY_REVENUE, Money.Currency.EUR),
            ANNUAL_ROI,
            PAYBACK_PERIOD,
            OCCUPANCY_RATE
        );

        var marketAnalysis = new MarketAnalysis(
            50,
            new Money(AVERAGE_DAILY_RATE, Money.Currency.EUR),
            BigDecimal.valueOf(OCCUPANCY_RATE),
            new Money(MONTHLY_REVENUE, Money.Currency.EUR),
            0.25,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.LOW
        );

        return new AnalysisResult(config, metrics, marketAnalysis, quality);
    }
}
