package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.Money;
import com.str.platform.scraping.infrastructure.persistence.entity.PriceSampleEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyAvailabilityEntity;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyAvailabilityRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPriceSampleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyDataAnalysisServiceTest {

    private static final UUID LOCATION_ID = UUID.fromString("b9353cb4-26fb-44d5-b009-4fb558fead80");

    @Mock
    private JpaPriceSampleRepository priceSampleRepository;

    @Mock
    private JpaPropertyAvailabilityRepository availabilityRepository;

    @InjectMocks
    private PropertyDataAnalysisService sut;

    @Nested
    @DisplayName("Average Daily Rate Calculation")
    class AverageDailyRateCalculation {

        @Test
        void shouldCalculateADRFromPriceSamples() {
            // Given
            List<PriceSampleEntity> samples = List.of(
                createPriceSample(new BigDecimal("300"), 3),
                createPriceSample(new BigDecimal("500"), 5),
                createPriceSample(new BigDecimal("600"), 6)
            );
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(samples);

            // When
            Money result = sut.calculateAverageDailyRate(LOCATION_ID);

            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(adr -> {
                    assertThat(adr.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
                    assertThat(adr.getCurrency()).isEqualTo(Money.Currency.EUR);
                });
        }

        @Test
        void shouldReturnNullWhenNoPriceSamples() {
            // Given
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(List.of());

            // When
            Money result = sut.calculateAverageDailyRate(LOCATION_ID);

            // Then
            assertThat(result)
                .as("Should return null when no price samples available")
                .isNull();
        }

        @Test
        void shouldFilterOutInvalidSamples() {
            // Given
            List<PriceSampleEntity> samples = List.of(
                createPriceSample(new BigDecimal("300"), 0),
                createPriceSample(new BigDecimal("400"), 4),
                createPriceSample(new BigDecimal("500"), 5)
            );
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(samples);

            // When
            Money result = sut.calculateAverageDailyRate(LOCATION_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        void shouldUseMedianForRobustness() {
            // Given
            List<PriceSampleEntity> samples = List.of(
                createPriceSample(new BigDecimal("100"), 1),
                createPriceSample(new BigDecimal("200"), 2),
                createPriceSample(new BigDecimal("300"), 3),
                createPriceSample(new BigDecimal("400"), 4),
                createPriceSample(new BigDecimal("1000"), 10)
            );
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(samples);

            // When
            Money result = sut.calculateAverageDailyRate(LOCATION_ID);

            // Then
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("Seasonality Index Calculation")
    class SeasonalityIndexCalculation {

        @Test
        void shouldCalculateSeasonalityFromMonthlyVariation() {
            // Given
            List<PriceSampleEntity> samples = createSeasonalPriceSamples();
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(samples);

            // When
            double result = sut.calculateSeasonalityIndex(LOCATION_ID);

            // Then
            assertThat(result)
                .as("Seasonality should reflect monthly variation")
                .isGreaterThan(0.0);
        }

        @Test
        void shouldReturnZeroWhenInsufficientSamples() {
            // Given
            List<PriceSampleEntity> samples = List.of(
                createPriceSample(new BigDecimal("100"), 1),
                createPriceSample(new BigDecimal("100"), 1)
            );
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(samples);

            // When
            double result = sut.calculateSeasonalityIndex(LOCATION_ID);

            // Then
            assertThat(result)
                .as("Should return 0 when insufficient samples")
                .isZero();
        }

        @Test
        void shouldReturnZeroWhenTooFewMonths() {
            // Given
            List<PriceSampleEntity> samples = createSamplesForFewMonths();
            when(priceSampleRepository.findByLocationId(LOCATION_ID)).thenReturn(samples);

            // When
            double result = sut.calculateSeasonalityIndex(LOCATION_ID);

            // Then
            assertThat(result)
                .as("Should return 0 when data spans too few months")
                .isZero();
        }
    }

    @Nested
    @DisplayName("Occupancy Calculation")
    class OccupancyCalculation {

        @Test
        void shouldCalculateAverageOccupancy() {
            // Given
            List<PropertyAvailabilityEntity> availabilityData = List.of(
                createAvailability(new BigDecimal("0.65")),
                createAvailability(new BigDecimal("0.70")),
                createAvailability(new BigDecimal("0.75"))
            );
            when(availabilityRepository.findLatestByLocationId(LOCATION_ID)).thenReturn(availabilityData);

            // When
            BigDecimal result = sut.calculateOccupancy(LOCATION_ID);

            // Then
            assertThat(result)
                .isNotNull()
                .isEqualByComparingTo(new BigDecimal("0.7000"));
        }

        @Test
        void shouldReturnNullWhenNoAvailabilityData() {
            // Given
            when(availabilityRepository.findLatestByLocationId(LOCATION_ID)).thenReturn(List.of());

            // When
            BigDecimal result = sut.calculateOccupancy(LOCATION_ID);

            // Then
            assertThat(result)
                .as("Should return null when no availability data")
                .isNull();
        }

        @Test
        void shouldHandleSingleDataPoint() {
            // Given
            List<PropertyAvailabilityEntity> availabilityData = List.of(
                createAvailability(new BigDecimal("0.80"))
            );
            when(availabilityRepository.findLatestByLocationId(LOCATION_ID)).thenReturn(availabilityData);

            // When
            BigDecimal result = sut.calculateOccupancy(LOCATION_ID);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("0.8000"));
        }
    }

    private PriceSampleEntity createPriceSample(BigDecimal price, int nights) {
        PriceSampleEntity entity = new PriceSampleEntity();
        entity.setPrice(price);
        entity.setNumberOfNights(nights);
        entity.setSearchDateStart(LocalDate.of(2025, 1, 15));
        entity.setSearchDateEnd(LocalDate.of(2025, 1, 15).plusDays(nights));
        return entity;
    }

    private List<PriceSampleEntity> createSeasonalPriceSamples() {
        return List.of(
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("220"), 2, 2),
            createPriceSampleForMonth(new BigDecimal("240"), 2, 3),
            createPriceSampleForMonth(new BigDecimal("400"), 2, 7),
            createPriceSampleForMonth(new BigDecimal("420"), 2, 8),
            createPriceSampleForMonth(new BigDecimal("440"), 2, 9),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 10),
            createPriceSampleForMonth(new BigDecimal("220"), 2, 11),
            createPriceSampleForMonth(new BigDecimal("180"), 2, 12),
            createPriceSampleForMonth(new BigDecimal("210"), 2, 4),
            createPriceSampleForMonth(new BigDecimal("230"), 2, 5),
            createPriceSampleForMonth(new BigDecimal("250"), 2, 6)
        );
    }

    private PriceSampleEntity createPriceSampleForMonth(BigDecimal price, int nights, int month) {
        PriceSampleEntity entity = new PriceSampleEntity();
        entity.setPrice(price);
        entity.setNumberOfNights(nights);
        entity.setSearchDateStart(LocalDate.of(2025, month, 15));
        entity.setSearchDateEnd(LocalDate.of(2025, month, 15).plusDays(nights));
        return entity;
    }

    private List<PriceSampleEntity> createSamplesForFewMonths() {
        return List.of(
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 2),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 2),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 2),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 2),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 2),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 1),
            createPriceSampleForMonth(new BigDecimal("200"), 2, 2)
        );
    }

    private PropertyAvailabilityEntity createAvailability(BigDecimal occupancy) {
        PropertyAvailabilityEntity entity = new PropertyAvailabilityEntity();
        entity.setEstimatedOccupancy(occupancy);
        entity.setMonth("2025-01");
        return entity;
    }
}
