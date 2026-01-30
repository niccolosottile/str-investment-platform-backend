package com.str.platform.scraping.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PropertyAvailability value object.
 * Tests occupancy calculation logic.
 */
class PropertyAvailabilityTest {
    
    private static final YearMonth JUNE_2026 = YearMonth.of(2026, 6);
    private static final int DAYS_IN_JUNE = 30;
    
    @Nested
    @DisplayName("Occupancy Calculation")
    class OccupancyCalculation {
        
        @Test
        void shouldCalculateOccupancyCorrectly() {
            // Given - 30 total days, 20 booked, 5 blocked = 20/(30-5) = 80%
            double occupancy = PropertyAvailability.calculateOccupancy(30, 5, 20, 5);
            
            // Then
            assertThat(occupancy)
                .as("Occupancy should be booked/(total-blocked)")
                .isCloseTo(0.80, within(0.01));
        }
        
        @Test
        void shouldReturnZeroOccupancyWhenAllDaysBlocked() {
            // Given - All 30 days blocked
            double occupancy = PropertyAvailability.calculateOccupancy(30, 0, 0, 30);
            
            // Then
            assertThat(occupancy)
                .as("Occupancy should be zero when all days are blocked")
                .isEqualTo(0.0);
        }
        
        @Test
        void shouldReturnZeroOccupancyWhenBlockedExceedsTotal() {
            // Given - More blocked days than total (edge case)
            double occupancy = PropertyAvailability.calculateOccupancy(30, 0, 0, 35);
            
            // Then
            assertThat(occupancy).isEqualTo(0.0);
        }
        
        @Test
        void shouldCalculate100PercentOccupancy() {
            // Given - All available days are booked
            double occupancy = PropertyAvailability.calculateOccupancy(30, 0, 25, 5);
            
            // Then
            assertThat(occupancy)
                .as("100% occupancy when all non-blocked days are booked")
                .isCloseTo(1.0, within(0.01));
        }
        
        @ParameterizedTest(name = "{0} booked out of {1} available → {2}% occupancy")
        @CsvSource({
            "15, 30, 0.50",  // 50% occupancy
            "10, 30, 0.33",  // 33% occupancy
            "0, 30, 0.00",   // 0% occupancy
            "30, 30, 1.00"   // 100% occupancy
        })
        void shouldCalculateVariousOccupancyRates(int booked, int total, double expectedOccupancy) {
            // When
            double occupancy = PropertyAvailability.calculateOccupancy(total, total - booked, booked, 0);
            
            // Then
            assertThat(occupancy).isCloseTo(expectedOccupancy, within(0.01));
        }
    }
    
    @Nested
    @DisplayName("PropertyAvailability Creation")
    class PropertyAvailabilityCreation {
        
        @Test
        void shouldCreateAvailabilityWithCorrectData() {
            // Given
            double occupancy = PropertyAvailability.calculateOccupancy(30, 10, 18, 2);
            
            // When
            var availability = new PropertyAvailability(
                JUNE_2026,
                DAYS_IN_JUNE,
                10,  // available
                18,  // booked
                2,   // blocked
                occupancy
            );
            
            // Then
            assertThat(availability)
                .satisfies(a -> {
                    assertThat(a.getMonth()).isEqualTo(JUNE_2026);
                    assertThat(a.getTotalDays()).isEqualTo(DAYS_IN_JUNE);
                    assertThat(a.getAvailableDays()).isEqualTo(10);
                    assertThat(a.getBookedDays()).isEqualTo(18);
                    assertThat(a.getBlockedDays()).isEqualTo(2);
                    assertThat(a.getEstimatedOccupancy()).isCloseTo(0.64, within(0.01));
                });
        }
    }
}
