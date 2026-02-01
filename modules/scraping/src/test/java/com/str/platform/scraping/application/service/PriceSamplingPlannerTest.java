package com.str.platform.scraping.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PriceSamplingPlanner")
class PriceSamplingPlannerTest {

    private final PriceSamplingPlanner sut = new PriceSamplingPlanner();

    @Nested
    @DisplayName("Generate Price Sample Periods")
    class GeneratePriceSamplePeriods {

        @Test
        void shouldGenerate12MonthlySamples() {
            // When
            List<PriceSamplingPlanner.DateRange> periods = sut.generatePriceSamplePeriods();

            // Then
            assertThat(periods).hasSize(12);
        }

        @Test
        void shouldStartFrom30DaysInFuture() {
            // Given
            LocalDate expectedStart = LocalDate.now().plusDays(30);

            // When
            List<PriceSamplingPlanner.DateRange> periods = sut.generatePriceSamplePeriods();

            // Then
            assertThat(periods.get(0).start()).isEqualTo(expectedStart);
        }

        @Test
        void shouldHave7NightDuration() {
            // When
            List<PriceSamplingPlanner.DateRange> periods = sut.generatePriceSamplePeriods();

            // Then
            for (PriceSamplingPlanner.DateRange period : periods) {
                long nights = java.time.temporal.ChronoUnit.DAYS.between(period.start(), period.end());
                assertThat(nights).isEqualTo(7);
            }
        }

        @Test
        void shouldHave30DayIntervalBetweenSamples() {
            // When
            List<PriceSamplingPlanner.DateRange> periods = sut.generatePriceSamplePeriods();

            // Then
            for (int i = 0; i < periods.size() - 1; i++) {
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    periods.get(i).start(),
                    periods.get(i + 1).start()
                );
                assertThat(daysBetween).isEqualTo(30);
            }
        }

        @Test
        void shouldCoverEntireYear() {
            // When
            List<PriceSamplingPlanner.DateRange> periods = sut.generatePriceSamplePeriods();

            // Then
            LocalDate firstStart = periods.get(0).start();
            LocalDate lastEnd = periods.get(11).end();
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstStart, lastEnd);
            assertThat(totalDays).isGreaterThan(330);
        }
    }

    @Nested
    @DisplayName("Default Search Range")
    class DefaultSearchRange {

        @Test
        void shouldStartFrom30DaysInFuture() {
            // Given
            LocalDate expectedStart = LocalDate.now().plusDays(30);

            // When
            PriceSamplingPlanner.DateRange range = sut.defaultSearchRange();

            // Then
            assertThat(range.start()).isEqualTo(expectedStart);
        }

        @Test
        void shouldHave7NightDuration() {
            // When
            PriceSamplingPlanner.DateRange range = sut.defaultSearchRange();

            // Then
            long nights = java.time.temporal.ChronoUnit.DAYS.between(range.start(), range.end());
            assertThat(nights).isEqualTo(7);
        }
    }
}
