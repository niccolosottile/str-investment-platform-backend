package com.str.platform.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarketAnalysis")
class MarketAnalysisTest {

    private static final int TOTAL_LISTINGS = 150;
    private static final Money AVERAGE_DAILY_RATE = new Money(new BigDecimal("120.00"), Money.Currency.EUR);
    private static final BigDecimal OCCUPANCY_RATE = new BigDecimal("0.68");
    private static final Money ESTIMATED_MONTHLY_REVENUE = new Money(new BigDecimal("2448.00"), Money.Currency.EUR);
    private static final double SEASONALITY_INDEX = 0.35;

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        void shouldCreateMarketAnalysisWithAllFields() {
            var marketAnalysis = new MarketAnalysis(
                TOTAL_LISTINGS,
                AVERAGE_DAILY_RATE,
                OCCUPANCY_RATE,
                ESTIMATED_MONTHLY_REVENUE,
                SEASONALITY_INDEX,
                MarketAnalysis.GrowthTrend.INCREASING,
                MarketAnalysis.CompetitionDensity.MEDIUM
            );

            assertThat(marketAnalysis.getTotalListings()).isEqualTo(TOTAL_LISTINGS);
            assertThat(marketAnalysis.getAverageDailyRate()).isEqualTo(AVERAGE_DAILY_RATE);
            assertThat(marketAnalysis.getAverageOccupancyRate()).isEqualByComparingTo(OCCUPANCY_RATE);
            assertThat(marketAnalysis.getEstimatedMonthlyRevenue()).isEqualTo(ESTIMATED_MONTHLY_REVENUE);
            assertThat(marketAnalysis.getSeasonalityIndex()).isEqualTo(SEASONALITY_INDEX);
            assertThat(marketAnalysis.getGrowthTrend()).isEqualTo(MarketAnalysis.GrowthTrend.INCREASING);
            assertThat(marketAnalysis.getCompetitionDensity()).isEqualTo(MarketAnalysis.CompetitionDensity.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Growth Trend Classifications")
    class GrowthTrendClassifications {

        @Test
        void shouldSupportIncreasingTrend() {
            var marketAnalysis = createMarketAnalysisWithTrend(MarketAnalysis.GrowthTrend.INCREASING);

            assertThat(marketAnalysis.getGrowthTrend()).isEqualTo(MarketAnalysis.GrowthTrend.INCREASING);
        }

        @Test
        void shouldSupportStableTrend() {
            var marketAnalysis = createMarketAnalysisWithTrend(MarketAnalysis.GrowthTrend.STABLE);

            assertThat(marketAnalysis.getGrowthTrend()).isEqualTo(MarketAnalysis.GrowthTrend.STABLE);
        }

        @Test
        void shouldSupportDecliningTrend() {
            var marketAnalysis = createMarketAnalysisWithTrend(MarketAnalysis.GrowthTrend.DECLINING);

            assertThat(marketAnalysis.getGrowthTrend()).isEqualTo(MarketAnalysis.GrowthTrend.DECLINING);
        }
    }

    @Nested
    @DisplayName("Competition Density Classifications")
    class CompetitionDensityClassifications {

        @Test
        void shouldSupportLowCompetition() {
            var marketAnalysis = createMarketAnalysisWithDensity(MarketAnalysis.CompetitionDensity.LOW);

            assertThat(marketAnalysis.getCompetitionDensity()).isEqualTo(MarketAnalysis.CompetitionDensity.LOW);
        }

        @Test
        void shouldSupportMediumCompetition() {
            var marketAnalysis = createMarketAnalysisWithDensity(MarketAnalysis.CompetitionDensity.MEDIUM);

            assertThat(marketAnalysis.getCompetitionDensity()).isEqualTo(MarketAnalysis.CompetitionDensity.MEDIUM);
        }

        @Test
        void shouldSupportHighCompetition() {
            var marketAnalysis = createMarketAnalysisWithDensity(MarketAnalysis.CompetitionDensity.HIGH);

            assertThat(marketAnalysis.getCompetitionDensity()).isEqualTo(MarketAnalysis.CompetitionDensity.HIGH);
        }
    }

    @Nested
    @DisplayName("Occupancy Rate Handling")
    class OccupancyRateHandling {

        @Test
        void shouldHandleZeroOccupancyRate() {
            var marketAnalysis = createMarketAnalysisWithOccupancy(BigDecimal.ZERO);

            assertThat(marketAnalysis.getAverageOccupancyRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldHandleFullOccupancyRate() {
            var marketAnalysis = createMarketAnalysisWithOccupancy(BigDecimal.ONE);

            assertThat(marketAnalysis.getAverageOccupancyRate()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        void shouldHandleTypicalOccupancyRate() {
            var marketAnalysis = createMarketAnalysisWithOccupancy(new BigDecimal("0.75"));

            assertThat(marketAnalysis.getAverageOccupancyRate()).isEqualByComparingTo(new BigDecimal("0.75"));
        }
    }

    @Nested
    @DisplayName("Seasonality Index Handling")
    class SeasonalityIndexHandling {

        @Test
        void shouldHandleNoSeasonality() {
            var marketAnalysis = createMarketAnalysisWithSeasonality(0.0);

            assertThat(marketAnalysis.getSeasonalityIndex()).isZero();
        }

        @Test
        void shouldHandleLowSeasonality() {
            var marketAnalysis = createMarketAnalysisWithSeasonality(0.2);

            assertThat(marketAnalysis.getSeasonalityIndex()).isEqualTo(0.2);
        }

        @Test
        void shouldHandleHighSeasonality() {
            var marketAnalysis = createMarketAnalysisWithSeasonality(1.5);

            assertThat(marketAnalysis.getSeasonalityIndex()).isEqualTo(1.5);
        }
    }

    private MarketAnalysis createMarketAnalysisWithTrend(MarketAnalysis.GrowthTrend trend) {
        return new MarketAnalysis(
            TOTAL_LISTINGS,
            AVERAGE_DAILY_RATE,
            OCCUPANCY_RATE,
            ESTIMATED_MONTHLY_REVENUE,
            SEASONALITY_INDEX,
            trend,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );
    }

    private MarketAnalysis createMarketAnalysisWithDensity(MarketAnalysis.CompetitionDensity density) {
        return new MarketAnalysis(
            TOTAL_LISTINGS,
            AVERAGE_DAILY_RATE,
            OCCUPANCY_RATE,
            ESTIMATED_MONTHLY_REVENUE,
            SEASONALITY_INDEX,
            MarketAnalysis.GrowthTrend.STABLE,
            density
        );
    }

    private MarketAnalysis createMarketAnalysisWithOccupancy(BigDecimal occupancyRate) {
        return new MarketAnalysis(
            TOTAL_LISTINGS,
            AVERAGE_DAILY_RATE,
            occupancyRate,
            ESTIMATED_MONTHLY_REVENUE,
            SEASONALITY_INDEX,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );
    }

    private MarketAnalysis createMarketAnalysisWithSeasonality(double seasonality) {
        return new MarketAnalysis(
            TOTAL_LISTINGS,
            AVERAGE_DAILY_RATE,
            OCCUPANCY_RATE,
            ESTIMATED_MONTHLY_REVENUE,
            seasonality,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );
    }
}
