package com.str.platform.scraping.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PriceSample value object.
 * Tests price sample creation and ADR calculation.
 */
class PriceSampleTest {
    
    private static final BigDecimal PRICE_600 = new BigDecimal("600.00");
    private static final String CURRENCY_EUR = "EUR";
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 6, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 6, 4); // 3 nights
    private static final int THREE_NIGHTS = 3;
    
    @Nested
    @DisplayName("Price Sample Creation")
    class PriceSampleCreation {
        
        @Test
        void shouldCreateValidPriceSample() {
            // Given
            Instant sampledAt = Instant.now();
            
            // When
            var sample = new PriceSample(
                PRICE_600,
                CURRENCY_EUR,
                CHECK_IN,
                CHECK_OUT,
                THREE_NIGHTS,
                sampledAt
            );
            
            // Then
            assertThat(sample)
                .satisfies(s -> {
                    assertThat(s.getPrice()).isEqualByComparingTo(PRICE_600);
                    assertThat(s.getCurrency()).isEqualTo(CURRENCY_EUR);
                    assertThat(s.getSearchDateStart()).isEqualTo(CHECK_IN);
                    assertThat(s.getSearchDateEnd()).isEqualTo(CHECK_OUT);
                    assertThat(s.getNumberOfNights()).isEqualTo(THREE_NIGHTS);
                    assertThat(s.getSampledAt()).isEqualTo(sampledAt);
                });
        }
    }
    
    @Nested
    @DisplayName("Average Daily Rate Calculation")
    class AverageDailyRateCalculation {
        
        @Test
        void shouldCalculateADRCorrectly() {
            // Given - €600 for 3 nights = €200/night
            var sample = createSample(PRICE_600, THREE_NIGHTS);
            
            // When
            BigDecimal adr = sample.getAverageDailyRate();
            
            // Then
            assertThat(adr)
                .as("ADR should be total price divided by number of nights")
                .isEqualByComparingTo("200.00");
        }
        
        @Test
        void shouldCalculateADRForSingleNight() {
            // Given - €150 for 1 night
            var sample = createSample(new BigDecimal("150.00"), 1);
            
            // When
            BigDecimal adr = sample.getAverageDailyRate();
            
            // Then
            assertThat(adr).isEqualByComparingTo("150.00");
        }
        
        @Test
        void shouldCalculateADRForWeekLongStay() {
            // Given - €1,050 for 7 nights = €150/night
            var sample = createSample(new BigDecimal("1050.00"), 7);
            
            // When
            BigDecimal adr = sample.getAverageDailyRate();
            
            // Then
            assertThat(adr).isEqualByComparingTo("150.00");
        }
        
        @Test
        void shouldRoundADRToTwoDecimalPlaces() {
            // Given - €100 for 3 nights = 33.333...
            var sample = createSample(new BigDecimal("100.00"), 3);
            
            // When
            BigDecimal adr = sample.getAverageDailyRate();
            
            // Then
            assertThat(adr)
                .as("ADR should be rounded to 2 decimal places")
                .isEqualByComparingTo("33.33");
        }
        
        @Test
        void shouldReturnZeroWhenNightsIsZero() {
            // Given
            var sample = createSample(PRICE_600, 0);
            
            // When
            BigDecimal adr = sample.getAverageDailyRate();
            
            // Then
            assertThat(adr)
                .as("ADR should be zero when nights is zero")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
        
        @Test
        void shouldReturnZeroWhenNightsIsNegative() {
            // Given
            var sample = createSample(PRICE_600, -3);
            
            // When
            BigDecimal adr = sample.getAverageDailyRate();
            
            // Then
            assertThat(adr)
                .as("ADR should be zero when nights is negative")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
    
    private PriceSample createSample(BigDecimal price, int nights) {
        return new PriceSample(
            price,
            CURRENCY_EUR,
            CHECK_IN,
            CHECK_OUT,
            nights,
            Instant.now()
        );
    }
}
