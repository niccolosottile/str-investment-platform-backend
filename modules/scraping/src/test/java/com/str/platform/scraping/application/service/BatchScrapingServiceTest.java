package com.str.platform.scraping.application.service;

import com.str.platform.location.infrastructure.persistence.repository.JpaLocationRepository;
import com.str.platform.scraping.application.dto.BatchScrapingRequest;
import com.str.platform.scraping.application.dto.BatchScrapingStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchScrapingService")
class BatchScrapingServiceTest {

    @Mock
    private ScrapingOrchestrationService orchestrationService;

    @Mock
    private JpaLocationRepository locationRepository;

    @InjectMocks
    private BatchScrapingService sut;

    @Nested
    @DisplayName("Validation - Empty Locations")
    class ValidationEmptyLocations {

        @Test
        void shouldRejectAllLocationsStrategyWhenNoLocationsExist() {
            // Given
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.ALL_LOCATIONS,
                10,
                7
            );
            when(locationRepository.findAll()).thenReturn(List.of());

            // When / Then
            assertThatThrownBy(() -> sut.scheduleBatchRefresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No locations available");
        }

        @Test
        void shouldRejectStaleOnlyStrategyWhenNoStaleLocationsExist() {
            // Given
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.STALE_ONLY,
                10,
                30
            );
            when(locationRepository.findStaleLocations(any(Instant.class))).thenReturn(List.of());

            // When / Then
            assertThatThrownBy(() -> sut.scheduleBatchRefresh(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No locations available");
        }

        @Test
        void shouldNotStartBatchWhenValidationFails() {
            // Given
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.ALL_LOCATIONS,
                10,
                7
            );
            when(locationRepository.findAll()).thenReturn(List.of());

            // When
            try {
                sut.scheduleBatchRefresh(request);
            } catch (IllegalArgumentException e) {
                // Expected
            }

            // Then
            BatchScrapingStatusResponse status = sut.getBatchProgress();
            assertThat(status.status()).isEqualTo(BatchScrapingStatusResponse.BatchStatus.NOT_STARTED);
        }
    }

    @Nested
    @DisplayName("Get Batch Progress - Initial State")
    class GetBatchProgressInitialState {

        @Test
        void shouldReturnNotStartedStatusByDefault() {
            // When
            BatchScrapingStatusResponse status = sut.getBatchProgress();

            // Then
            assertThat(status.status()).isEqualTo(BatchScrapingStatusResponse.BatchStatus.NOT_STARTED);
        }

        @Test
        void shouldReturnNullBatchIdWhenNotStarted() {
            // When
            BatchScrapingStatusResponse status = sut.getBatchProgress();

            // Then
            assertThat(status.batchId()).isNull();
        }

        @Test
        void shouldReturnZeroTotalLocationsWhenNotStarted() {
            // When
            BatchScrapingStatusResponse status = sut.getBatchProgress();

            // Then
            assertThat(status.totalLocations()).isZero();
        }

        @Test
        void shouldReturnZeroCompletedLocationsWhenNotStarted() {
            // When
            BatchScrapingStatusResponse status = sut.getBatchProgress();

            // Then
            assertThat(status.completedLocations()).isZero();
        }

        @Test
        void shouldReturnZeroFailedLocationsWhenNotStarted() {
            // When
            BatchScrapingStatusResponse status = sut.getBatchProgress();

            // Then
            assertThat(status.failedLocations()).isZero();
        }
    }

    @Nested
    @DisplayName("Repository Interaction")
    class RepositoryInteraction {

        @Test
        void shouldQueryAllLocationsForAllLocationsStrategy() {
            // Given
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.ALL_LOCATIONS,
                10,
                7
            );
            when(locationRepository.findAll()).thenReturn(List.of());

            // When
            try {
                sut.scheduleBatchRefresh(request);
            } catch (IllegalArgumentException e) {
                // Expected due to empty list
            }

            // Then
            verify(locationRepository).findAll();
            verify(locationRepository, never()).findStaleLocations(any());
        }

        @Test
        void shouldQueryStaleLocationsForStaleOnlyStrategy() {
            // Given
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.STALE_ONLY,
                10,
                30
            );
            when(locationRepository.findStaleLocations(any(Instant.class))).thenReturn(List.of());

            // When
            try {
                sut.scheduleBatchRefresh(request);
            } catch (IllegalArgumentException e) {
                // Expected due to empty list
            }

            // Then
            verify(locationRepository).findStaleLocations(any(Instant.class));
            verify(locationRepository, never()).findAll();
        }

        @Test
        void shouldUseThresholdWhenQueryingStaleLocations() {
            // Given
            int thresholdDays = 14;
            BatchScrapingRequest request = new BatchScrapingRequest(
                BatchScrapingRequest.BatchStrategy.STALE_ONLY,
                10,
                thresholdDays
            );
            when(locationRepository.findStaleLocations(any(Instant.class))).thenReturn(List.of());

            // When
            try {
                sut.scheduleBatchRefresh(request);
            } catch (IllegalArgumentException e) {
                // Expected
            }

            // Then
            verify(locationRepository).findStaleLocations(any(Instant.class));
        }
    }
}
