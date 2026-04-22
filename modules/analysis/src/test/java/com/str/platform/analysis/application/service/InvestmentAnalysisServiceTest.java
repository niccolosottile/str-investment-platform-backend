package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InvestmentAnalysisService.
 * Tests core business logic for ROI calculation, revenue projections, and payback period.
 */
class InvestmentAnalysisServiceTest {
    
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final Money DAILY_RATE_100 = Money.euros(100);
    private static final Money BUDGET_BUY = Money.euros(200_000);
    private static final Money BUDGET_RENT = Money.euros(10_000);
    private static final double OCCUPANCY_65_PERCENT = 0.65;
    
    private InvestmentAnalysisService sut;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sut = new InvestmentAnalysisService();
    }
    
    @Nested
    @DisplayName("Monthly Revenue Calculation")
    class MonthlyRevenueCalculation {
        
        @Test
        void shouldCalculateMonthlyRevenueWithOperatingCosts() {
            // Given - €100 daily rate, 65% occupancy
            // Gross = 100 × 30 × 0.65 = 1,950
            // Net = 1,950 × 0.75 (after 25% operating costs) = 1,462.50
            
            // When
            Money result = sut.calculateMonthlyRevenue(DAILY_RATE_100, OCCUPANCY_65_PERCENT);
            
            // Then
            assertThat(result.getAmount())
                .as("Monthly revenue should account for 25% operating costs")
                .isEqualByComparingTo("1462.50");
        }
        
        @Test
        void shouldReturnZeroRevenueWithZeroOccupancy() {
            // When
            Money result = sut.calculateMonthlyRevenue(DAILY_RATE_100, 0.0);
            
            // Then
            assertThat(result.getAmount())
                .as("Zero occupancy should yield zero revenue")
                .isEqualByComparingTo("0.00");
        }
        
        @ParameterizedTest(name = "Occupancy {0}% should yield revenue")
        @CsvSource({
            "0.50, 1125.00",   // 50% occupancy
            "0.80, 1800.00",   // 80% occupancy
            "1.00, 2250.00"    // 100% occupancy
        })
        void shouldCalculateRevenueForVariousOccupancyRates(double occupancy, String expectedRevenue) {
            // When
            Money result = sut.calculateMonthlyRevenue(DAILY_RATE_100, occupancy);
            
            // Then
            assertThat(result.getAmount())
                .isEqualByComparingTo(expectedRevenue);
        }
        
        @Test
        void shouldPreserveCurrencyInCalculation() {
            // When
            Money result = sut.calculateMonthlyRevenue(DAILY_RATE_100, OCCUPANCY_65_PERCENT);
            
            // Then
            assertThat(result.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
    }
    
    @Nested
    @DisplayName("Investment Metrics Calculation")
    class InvestmentMetricsCalculation {
        
        @Test
        void shouldCalculateMetricsWithThreeRevenueScenarios() {
            // Given
            var config = createBuyConfiguration();
            var marketAnalysis = createMarketAnalysis(DAILY_RATE_100, OCCUPANCY_65_PERCENT);
            
            // When
            InvestmentMetrics metrics = sut.calculateMetrics(config, marketAnalysis);
            
            // Then
            assertThat(metrics)
                .satisfies(m -> {
                    // Conservative: 65% × 0.85 = 55.25%
                    assertThat(m.getMonthlyRevenueConservative().getAmount())
                        .as("Conservative revenue (85% of expected occupancy)")
                        .isGreaterThan(BigDecimal.ZERO);
                    
                    // Expected: 65%
                    assertThat(m.getMonthlyRevenueExpected().getAmount())
                        .as("Expected revenue")
                        .isEqualByComparingTo("1462.50");
                    
                    // Optimistic: 65% × 1.15 = 74.75%
                    assertThat(m.getMonthlyRevenueOptimistic().getAmount())
                        .as("Optimistic revenue (115% of expected occupancy)")
                        .isGreaterThan(m.getMonthlyRevenueExpected().getAmount());
                });
        }
        
        @Test
        void shouldCalculatePositiveROIForViableInvestment() {
            // Given
            var config = createBuyConfiguration();
            var marketAnalysis = createMarketAnalysis(DAILY_RATE_100, OCCUPANCY_65_PERCENT);
            
            // When
            InvestmentMetrics metrics = sut.calculateMetrics(config, marketAnalysis);
            
            // Then
            assertThat(metrics.getAnnualROI())
                .as("ROI should be positive for viable investment")
                .isGreaterThan(0.0);
        }
        
        @Test
        void shouldCalculateReasonablePaybackPeriod() {
            // Given
            var config = createBuyConfiguration();
            var marketAnalysis = createMarketAnalysis(DAILY_RATE_100, OCCUPANCY_65_PERCENT);
            
            // When
            InvestmentMetrics metrics = sut.calculateMetrics(config, marketAnalysis);
            
            // Then
            assertThat(metrics.getPaybackPeriodMonths())
                .as("Payback period should be reasonable")
                .isPositive();
        }

        @Test
        void shouldReturnActualPaybackPeriodWhenItExceedsTwentyYears() {
            // Given
            var config = createBuyConfiguration();
            var marketAnalysis = createMarketAnalysis(Money.euros(40), 0.35);

            // When
            InvestmentMetrics metrics = sut.calculateMetrics(config, marketAnalysis);

            // Then
            assertThat(metrics.getPaybackPeriodMonths())
                .as("Payback period should not be capped at 20 years")
                .isGreaterThan(240);
        }
        
        @Test
        void shouldReflectMarketOccupancyRate() {
            // Given
            double marketOccupancy = 0.72;
            var config = createBuyConfiguration();
            var marketAnalysis = createMarketAnalysis(DAILY_RATE_100, marketOccupancy);
            
            // When
            InvestmentMetrics metrics = sut.calculateMetrics(config, marketAnalysis);
            
            // Then
            assertThat(metrics.getOccupancyRate())
                .as("Metrics should use market's average occupancy")
                .isEqualTo(marketOccupancy);
        }
    }
    
    @Nested
    @DisplayName("Data Quality Assessment")
    class DataQualityAssessment {
        
        @Test
        void shouldDetermineHighQualityWithDeepCoverage() {
            var quality = sut.determineDataQuality(new AnalysisDataCoverage(
                75,
                180,
                68,
                9,
                220,
                70,
                9
            ));
            
            assertThat(quality).isEqualTo(AnalysisResult.DataQuality.HIGH);
        }
        
        @Test
        void shouldDetermineMediumQualityWithModerateCoverage() {
            var quality = sut.determineDataQuality(new AnalysisDataCoverage(
                25,
                36,
                18,
                3,
                42,
                19,
                3
            ));
            
            assertThat(quality).isEqualTo(AnalysisResult.DataQuality.MEDIUM);
        }
        
        @Test
        void shouldDetermineLowQualityWhenTimeDepthIsThin() {
            var quality = sut.determineDataQuality(new AnalysisDataCoverage(
                117,
                129,
                117,
                1,
                117,
                117,
                1
            ));
            
            assertThat(quality).isEqualTo(AnalysisResult.DataQuality.LOW);
        }
        
        @Test
        void shouldDetermineLowQualityWhenCoverageIsSparse() {
            var quality = sut.determineDataQuality(new AnalysisDataCoverage(
                18,
                10,
                8,
                2,
                12,
                9,
                1
            ));

            assertThat(quality).isEqualTo(AnalysisResult.DataQuality.LOW);
        }
    }
    
    @Nested
    @DisplayName("ROI Calculation for Investment Types")
    class ROICalculation {
        
        @Test
        void shouldIncludePropertyAppreciationForBuyInvestments() {
            // Given - BUY investment includes 2% appreciation
            var buyConfig = createBuyConfiguration();
            var marketAnalysis = createMarketAnalysis(Money.euros(150), 0.70);
            
            // When
            InvestmentMetrics metrics = sut.calculateMetrics(buyConfig, marketAnalysis);
            
            // Then - ROI should account for rental income + 2% property appreciation
            assertThat(metrics.getAnnualROI())
                .as("BUY investment should include property appreciation in ROI")
                .isGreaterThan(2.0); // At minimum, the appreciation rate
        }
        
        @Test
        void shouldExcludeAppreciationForRentInvestments() {
            // Given - RENT investment (rental arbitrage)
            var rentConfig = new InvestmentConfiguration(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.RENT,
                BUDGET_RENT,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.MAX_ROI
            );
            var marketAnalysis = createMarketAnalysis(Money.euros(80), 0.70);
            
            // When
            InvestmentMetrics metrics = sut.calculateMetrics(rentConfig, marketAnalysis);
            
            // Then - ROI should only reflect rental income
            assertThat(metrics.getAnnualROI())
                .as("RENT investment ROI based only on rental income")
                .isGreaterThan(0.0);
        }
    }
    
    private InvestmentConfiguration createBuyConfiguration() {
        return new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            BUDGET_BUY,
            InvestmentConfiguration.PropertyType.APARTMENT,
            InvestmentConfiguration.InvestmentGoal.MAX_ROI
        );
    }
    
    private MarketAnalysis createMarketAnalysis(Money dailyRate, double occupancy) {
        Money monthlyRevenue = dailyRate.multiply(30 * occupancy);
        
        return new MarketAnalysis(
            50,  // totalListings
            dailyRate,
            BigDecimal.valueOf(occupancy),
            monthlyRevenue,
            0.15, // seasonalityIndex
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.MEDIUM
        );
    }
}
