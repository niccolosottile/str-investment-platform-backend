package com.str.platform.location.application.service;

import com.str.platform.location.domain.model.Distance;
import com.str.platform.location.infrastructure.external.mapbox.MapboxClient;
import com.str.platform.location.infrastructure.external.mapbox.dto.DirectionsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrivingTimeService")
class DrivingTimeServiceTest {

    @Mock
    private MapboxClient mapboxClient;

    @InjectMocks
    private DrivingTimeService sut;

    private static final double MILAN_LAT = 45.4642;
    private static final double MILAN_LNG = 9.1900;
    private static final double ROME_LAT = 41.9028;
    private static final double ROME_LNG = 12.4964;

    @Nested
    @DisplayName("Driving Time Calculation")
    class DrivingTimeCalculation {

        @Test
        void shouldCalculateDrivingTimeFromMapboxApi() {
            // Given
            DirectionsResponse.Route route = new DirectionsResponse.Route();
            route.setDistance(570000.0);
            route.setDuration(21600.0);
            DirectionsResponse response = new DirectionsResponse();
            response.setRoutes(List.of(route));
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(response);

            // When
            Distance result = sut.calculateDrivingTime(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getKilometers()).isEqualTo(570.0);
            assertThat(result.getDrivingTimeMinutes()).isEqualTo(360);
            verify(mapboxClient).getDirections(MILAN_LNG, MILAN_LAT, ROME_LNG, ROME_LAT);
        }

        @Test
        void shouldFallbackToHaversineWhenNoRouteFound() {
            // Given
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(null);

            // When
            Distance result = sut.calculateDrivingTime(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getKilometers()).isGreaterThan(0);
            assertThat(result.getDrivingTimeMinutes()).isNull();
        }

        @Test
        void shouldFallbackToHaversineOnApiError() {
            // Given
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("API Error"));

            // When
            Distance result = sut.calculateDrivingTime(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getKilometers()).isGreaterThan(0);
            assertThat(result.getDrivingTimeMinutes()).isNull();
        }
    }

    @Nested
    @DisplayName("Batch Driving Time Calculation")
    class BatchDrivingTimeCalculation {

        @Test
        void shouldCalculateDrivingTimesForMultipleDestinations() {
            // Given
            List<DrivingTimeService.DestinationCoordinate> destinations = List.of(
                new DrivingTimeService.DestinationCoordinate("dest1", ROME_LAT, ROME_LNG),
                new DrivingTimeService.DestinationCoordinate("dest2", 45.0, 9.0)
            );
            
            DirectionsResponse.Route route1 = new DirectionsResponse.Route();
            route1.setDistance(570000.0);
            route1.setDuration(21600.0);
            DirectionsResponse.Route route2 = new DirectionsResponse.Route();
            route2.setDistance(50000.0);
            route2.setDuration(3600.0);
            DirectionsResponse response1 = new DirectionsResponse();
            response1.setRoutes(List.of(route1));
            DirectionsResponse response2 = new DirectionsResponse();
            response2.setRoutes(List.of(route2));
            
            when(mapboxClient.getDirections(eq(MILAN_LNG), eq(MILAN_LAT), eq(ROME_LNG), eq(ROME_LAT)))
                .thenReturn(response1);
            when(mapboxClient.getDirections(eq(MILAN_LNG), eq(MILAN_LAT), eq(9.0), eq(45.0)))
                .thenReturn(response2);

            // When
            Map<String, Distance> results = sut.batchCalculateDrivingTimes(MILAN_LAT, MILAN_LNG, destinations);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get("dest1").getKilometers()).isEqualTo(570.0);
            assertThat(results.get("dest2").getKilometers()).isEqualTo(50.0);
        }

        @Test
        void shouldHandleFailuresInBatchCalculation() {
            // Given
            List<DrivingTimeService.DestinationCoordinate> destinations = List.of(
                new DrivingTimeService.DestinationCoordinate("dest1", ROME_LAT, ROME_LNG),
                new DrivingTimeService.DestinationCoordinate("dest2", 45.0, 9.0)
            );
            
            DirectionsResponse.Route route = new DirectionsResponse.Route();
            route.setDistance(570000.0);
            route.setDuration(21600.0);
            DirectionsResponse response = new DirectionsResponse();
            response.setRoutes(List.of(route));
            when(mapboxClient.getDirections(eq(MILAN_LNG), eq(MILAN_LAT), eq(ROME_LNG), eq(ROME_LAT)))
                .thenReturn(response);
            when(mapboxClient.getDirections(eq(MILAN_LNG), eq(MILAN_LAT), eq(9.0), eq(45.0)))
                .thenThrow(new RuntimeException("API Error"));

            // When
            Map<String, Distance> results = sut.batchCalculateDrivingTimes(MILAN_LAT, MILAN_LNG, destinations);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get("dest1")).isNotNull();
            assertThat(results.get("dest2")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Driving Time Minutes")
    class DrivingTimeMinutes {

        @Test
        void shouldReturnDrivingTimeInMinutes() {
            // Given
            DirectionsResponse.Route route = new DirectionsResponse.Route();
            route.setDistance(570000.0);
            route.setDuration(21600.0);
            DirectionsResponse response = new DirectionsResponse();
            response.setRoutes(List.of(route));
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(response);

            // When
            Double minutes = sut.getDrivingTimeMinutes(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG);

            // Then
            assertThat(minutes).isEqualTo(360.0);
        }

        @Test
        void shouldReturnNullWhenNoDrivingTimeAvailable() {
            // Given
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(null);

            // When
            Double minutes = sut.getDrivingTimeMinutes(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG);

            // Then
            assertThat(minutes).isNull();
        }
    }

    @Nested
    @DisplayName("Driving Time Threshold Check")
    class DrivingTimeThresholdCheck {

        @Test
        void shouldReturnTrueWhenWithinThreshold() {
            // Given
            DirectionsResponse.Route route = new DirectionsResponse.Route();
            route.setDistance(50000.0);
            route.setDuration(3000.0);
            DirectionsResponse response = new DirectionsResponse();
            response.setRoutes(List.of(route));
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(response);

            // When
            boolean result = sut.isWithinDrivingTime(MILAN_LAT, MILAN_LNG, 45.0, 9.0, 60);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenExceedsThreshold() {
            // Given
            DirectionsResponse.Route route = new DirectionsResponse.Route();
            route.setDistance(570000.0);
            route.setDuration(21600.0);
            DirectionsResponse response = new DirectionsResponse();
            response.setRoutes(List.of(route));
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(response);

            // When
            boolean result = sut.isWithinDrivingTime(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG, 60);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseOnError() {
            // Given
            when(mapboxClient.getDirections(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("API Error"));

            // When
            boolean result = sut.isWithinDrivingTime(MILAN_LAT, MILAN_LNG, ROME_LAT, ROME_LNG, 60);

            // Then
            assertThat(result).isFalse();
        }
    }
}
