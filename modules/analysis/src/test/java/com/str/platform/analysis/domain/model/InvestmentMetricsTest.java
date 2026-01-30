package com.str.platform.analysis.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for InvestmentMetrics value object.
 * Tests metric calculations and investment viability assessment.
 */
class InvestmentMetricsTest {
    
    private static final Money MONTHLY_REVENUE_CONSERVATIVE = Money.euros(1200);
    private static final Money MONTHLY_REVENUE_EXPECTED = Money.euros(1500);
    private static final Money MONTHLY_REVENUE_OPTIMISTIC = Money.euros(1800);
    private static final double GOOD_ROI = 8.5;
    private static final int REASONABLE_PAYBACK = 60; // 5 years
    private static final double STANDARD_OCCUPANCY = 0.65;
        
    @Nested
    @DisplayName("Annual Revenue Calculation")
    class AnnualRevenueCalculation {
        
        @Test
        void shouldCalculateAnnualRevenueFromMonthlyExpected() {
            // Given
            var metrics = createStandardMetrics();
            
            // When
            var annualRevenue = metrics.getAnnualRevenue();
            
            // Then - Expected monthly × 12
            assertThat(annualRevenue.getAmount())
                .as("Annual revenue should be monthly expected × 12")
                .isEqualByComparingTo("18000"); // 1500 × 12
        }
        
        @Test
        void shouldPreserveCurrencyInAnnualRevenue() {
            // Given
            var metrics = createStandardMetrics();
            
            // When
            var annualRevenue = metrics.getAnnualRevenue();
            
            // Then
            assertThat(annualRevenue.getCurrency()).isEqualTo(Money.Currency.EUR);
        }
    }
        
    @Nested
    @DisplayName("Investment Viability")
    class InvestmentViability {
        
        @Test
        void shouldBeViableWithGoodROIAndReasonablePayback() {
            // Given - ROI > 5%, payback < 120 months
            var metrics = new InvestmentMetrics(
                MONTHLY_REVENUE_CONSERVATIVE,
                MONTHLY_REVENUE_EXPECTED,
                MONTHLY_REVENUE_OPTIMISTIC,
                8.0,   // 8% ROI (good)
                72,    // 6 years payback (reasonable)
                STANDARD_OCCUPANCY
            );
            
            // When
            boolean isViable = metrics.isViableInvestment();
            
            // Then
            assertThat(isViable)
                .as("Investment with 8% ROI and 6-year payback should be viable")
                .isTrue();
        }
        
        @Test
        void shouldNotBeViableWithLowROI() {
            // Given - ROI ≤ 5%
            var metrics = new InvestmentMetrics(
                MONTHLY_REVENUE_CONSERVATIVE,
                MONTHLY_REVENUE_EXPECTED,
                MONTHLY_REVENUE_OPTIMISTIC,
                4.5,  // Low ROI
                60,   // Good payback
                STANDARD_OCCUPANCY
            );
            
            // When
            boolean isViable = metrics.isViableInvestment();
            
            // Then
            assertThat(isViable)
                .as("Investment with ROI ≤ 5% should not be viable")
                .isFalse();
        }
        
        @Test
        void shouldNotBeViableWithLongPaybackPeriod() {
            // Given - Payback ≥ 120 months (10 years)
            var metrics = new InvestmentMetrics(
                MONTHLY_REVENUE_CONSERVATIVE,
                MONTHLY_REVENUE_EXPECTED,
                MONTHLY_REVENUE_OPTIMISTIC,
                7.0,   // Good ROI
                125,   // Too long payback
                STANDARD_OCCUPANCY
            );
            
            // When
            boolean isViable = metrics.isViableInvestment();
            
            // Then
            assertThat(isViable)
                .as("Investment with payback ≥ 10 years should not be viable")
                .isFalse();
        }
        
        @Test
        void shouldBeViableAtExactThresholds() {
            // Given - Exactly at viability thresholds
            var metrics = new InvestmentMetrics(
                MONTHLY_REVENUE_CONSERVATIVE,
                MONTHLY_REVENUE_EXPECTED,
                MONTHLY_REVENUE_OPTIMISTIC,
                5.1,   // Just above 5%
                119,   // Just under 120 months
                STANDARD_OCCUPANCY
            );
            
            // When
            boolean isViable = metrics.isViableInvestment();
            
            // Then
            assertThat(isViable)
                .as("Investment at exact viability boundaries should be viable")
                .isTrue();
        }
        
        @Test
        void shouldNotBeViableWhenBothCriteriaFail() {
            // Given
            var metrics = new InvestmentMetrics(
                MONTHLY_REVENUE_CONSERVATIVE,
                MONTHLY_REVENUE_EXPECTED,
                MONTHLY_REVENUE_OPTIMISTIC,
                3.0,   // Low ROI
                150,   // Long payback
                STANDARD_OCCUPANCY
            );
            
            // When
            boolean isViable = metrics.isViableInvestment();
            
            // Then
            assertThat(isViable).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Revenue Scenarios")
    class RevenueScenarios {
        
        @Test
        void shouldMaintainThreeRevenueProjections() {
            // Given
            var metrics = createStandardMetrics();
            
            // Then
            assertThat(metrics.getMonthlyRevenueConservative())
                .as("Conservative projection")
                .isEqualTo(MONTHLY_REVENUE_CONSERVATIVE);
            
            assertThat(metrics.getMonthlyRevenueExpected())
                .as("Expected projection")
                .isEqualTo(MONTHLY_REVENUE_EXPECTED);
            
            assertThat(metrics.getMonthlyRevenueOptimistic())
                .as("Optimistic projection")
                .isEqualTo(MONTHLY_REVENUE_OPTIMISTIC);
        }
        
        @Test
        void shouldReflectOccupancyRateAccurately() {
            // Given
            double specificOccupancy = 0.72;
            var metrics = new InvestmentMetrics(
                MONTHLY_REVENUE_CONSERVATIVE,
                MONTHLY_REVENUE_EXPECTED,
                MONTHLY_REVENUE_OPTIMISTIC,
                GOOD_ROI,
                REASONABLE_PAYBACK,
                specificOccupancy
            );
            
            // Then
            assertThat(metrics.getOccupancyRate()).isEqualTo(specificOccupancy);
        }
    }
        
    private InvestmentMetrics createStandardMetrics() {
        return new InvestmentMetrics(
            MONTHLY_REVENUE_CONSERVATIVE,
            MONTHLY_REVENUE_EXPECTED,
            MONTHLY_REVENUE_OPTIMISTIC,
            GOOD_ROI,
            REASONABLE_PAYBACK,
            STANDARD_OCCUPANCY
        );
    }
}
