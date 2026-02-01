package com.str.platform.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalysisResult")
class AnalysisResultTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        void shouldCreateAnalysisResultWithAllComponents() {
            var config = createInvestmentConfiguration();
            var metrics = createInvestmentMetrics();
            var marketAnalysis = createMarketAnalysis();

            var result = new AnalysisResult(
                config,
                metrics,
                marketAnalysis,
                AnalysisResult.DataQuality.HIGH
            );

            assertThat(result.getConfiguration()).isEqualTo(config);
            assertThat(result.getMetrics()).isEqualTo(metrics);
            assertThat(result.getMarketAnalysis()).isEqualTo(marketAnalysis);
            assertThat(result.getDataQuality()).isEqualTo(AnalysisResult.DataQuality.HIGH);
        }

        @Test
        void shouldStartAsUncached() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);

            assertThat(result.isCached()).isFalse();
        }
    }

    @Nested
    @DisplayName("Caching Behavior")
    class CachingBehavior {

        @Test
        void shouldMarkResultAsCached() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);

            result.markAsCached();

            assertThat(result.isCached()).isTrue();
        }

        @Test
        void shouldAllowMarkingAsCachedMultipleTimes() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);

            result.markAsCached();
            result.markAsCached();

            assertThat(result.isCached()).isTrue();
        }
    }

    @Nested
    @DisplayName("Refresh Logic")
    class RefreshLogic {

        @Test
        void shouldNeedRefreshWhenOlderThanSixHours() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);
            setCreatedAt(result, LocalDateTime.now().minusHours(7));

            assertThat(result.needsRefresh()).isTrue();
        }

        @Test
        void shouldNotNeedRefreshWhenYoungerThanSixHours() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);
            setCreatedAt(result, LocalDateTime.now().minusHours(5));

            assertThat(result.needsRefresh()).isFalse();
        }

        @Test
        void shouldNeedRefreshWhenExactlySixHoursOld() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);
            setCreatedAt(result, LocalDateTime.now().minusHours(6).minusSeconds(1));

            assertThat(result.needsRefresh()).isTrue();
        }

        @Test
        void shouldNotNeedRefreshWhenJustCreated() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);
            setCreatedAt(result, LocalDateTime.now());

            assertThat(result.needsRefresh()).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Quality Levels")
    class DataQualityLevels {

        @Test
        void shouldSupportHighDataQuality() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.HIGH);

            assertThat(result.getDataQuality()).isEqualTo(AnalysisResult.DataQuality.HIGH);
        }

        @Test
        void shouldSupportMediumDataQuality() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.MEDIUM);

            assertThat(result.getDataQuality()).isEqualTo(AnalysisResult.DataQuality.MEDIUM);
        }

        @Test
        void shouldSupportLowDataQuality() {
            var result = createAnalysisResult(AnalysisResult.DataQuality.LOW);

            assertThat(result.getDataQuality()).isEqualTo(AnalysisResult.DataQuality.LOW);
        }
    }

    private AnalysisResult createAnalysisResult(AnalysisResult.DataQuality quality) {
        return new AnalysisResult(
            createInvestmentConfiguration(),
            createInvestmentMetrics(),
            createMarketAnalysis(),
            quality
        );
    }

    private InvestmentConfiguration createInvestmentConfiguration() {
        return new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            new Money(new BigDecimal("200000.00"), Money.Currency.EUR),
            InvestmentConfiguration.PropertyType.APARTMENT,
            InvestmentConfiguration.InvestmentGoal.MAX_ROI
        );
    }

    private InvestmentMetrics createInvestmentMetrics() {
        return new InvestmentMetrics(
            new Money(new BigDecimal("1500.00"), Money.Currency.EUR),
            new Money(new BigDecimal("2000.00"), Money.Currency.EUR),
            new Money(new BigDecimal("2500.00"), Money.Currency.EUR),
            8.5,
            48,
            0.72
        );
    }

    private MarketAnalysis createMarketAnalysis() {
        return new MarketAnalysis(
            150,
            new Money(new BigDecimal("120.00"), Money.Currency.EUR),
            new BigDecimal("0.68"),
            new Money(new BigDecimal("2448.00"), Money.Currency.EUR),
            0.35,
            MarketAnalysis.GrowthTrend.INCREASING,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );
    }

    private void setCreatedAt(AnalysisResult result, LocalDateTime createdAt) {
        result.restore(UUID.randomUUID(), createdAt, null);
    }
}
