package com.str.platform.location.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Coordinates value object.
 * Tests geographic coordinate validation and distance calculations.
 */
class CoordinatesTest {
    
    private static final double MILAN_LAT = 45.4642;
    private static final double MILAN_LNG = 9.1900;
    private static final double ROME_LAT = 41.9028;
    private static final double ROME_LNG = 12.4964;
    
    @Nested
    @DisplayName("Coordinate Creation")
    class CoordinateCreation {
        
        @Test
        void shouldCreateValidCoordinates() {
            // When
            var coordinates = new Coordinates(MILAN_LAT, MILAN_LNG);
            
            // Then
            assertThat(coordinates.getLatitude()).isEqualTo(MILAN_LAT);
            assertThat(coordinates.getLongitude()).isEqualTo(MILAN_LNG);
        }
        
        @ParameterizedTest(name = "Should reject latitude {0}")
        @CsvSource({
            "-90.1", "90.1", "-100", "100", "180", "-180"
        })
        void shouldRejectInvalidLatitudes(double invalidLatitude) {
            // When/Then
            assertThatThrownBy(() -> new Coordinates(invalidLatitude, MILAN_LNG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
        }
        
        @ParameterizedTest(name = "Should reject longitude {0}")
        @CsvSource({
            "-180.1", "180.1", "-200", "200", "360", "-360"
        })
        void shouldRejectInvalidLongitudes(double invalidLongitude) {
            // When/Then
            assertThatThrownBy(() -> new Coordinates(MILAN_LAT, invalidLongitude))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
        }
        
        @ParameterizedTest(name = "Latitude {0}, Longitude {1} should be valid")
        @CsvSource({
            "-90, -180",    // SW corner
            "90, 180",      // NE corner
            "0, 0",         // Equator/Prime Meridian
            "-45.5, 123.4", // Random valid point
        })
        void shouldAcceptBoundaryValues(double lat, double lng) {
            // When/Then
            assertThatNoException()
                .isThrownBy(() -> new Coordinates(lat, lng));
        }
    }
    
    @Nested
    @DisplayName("Distance Calculation")
    class DistanceCalculation {
        
        @Test
        void shouldCalculateDistanceBetweenMilanAndRome() {
            // Given
            var milan = new Coordinates(MILAN_LAT, MILAN_LNG);
            var rome = new Coordinates(ROME_LAT, ROME_LNG);
            
            // Expected: ~480 km (actual straight-line distance)
            double expectedDistance = 478.0;
            
            // When
            double calculatedDistance = milan.distanceTo(rome);
            
            // Then
            assertThat(calculatedDistance)
                .as("Distance from Milan to Rome")
                .isCloseTo(expectedDistance, within(10.0)); // Allow 10km tolerance for Haversine approximation
        }
        
        @Test
        void shouldReturnZeroDistanceForSameCoordinates() {
            // Given
            var milan = new Coordinates(MILAN_LAT, MILAN_LNG);
            var milanCopy = new Coordinates(MILAN_LAT, MILAN_LNG);
            
            // When
            double distance = milan.distanceTo(milanCopy);
            
            // Then
            assertThat(distance)
                .as("Distance from Milan to itself")
                .isCloseTo(0.0, within(0.01));
        }
        
        @Test
        void shouldCalculateSymmetricDistance() {
            // Given
            var pointA = new Coordinates(45.0, 9.0);
            var pointB = new Coordinates(46.0, 10.0);
            
            // When
            double distanceAtoB = pointA.distanceTo(pointB);
            double distanceBtoA = pointB.distanceTo(pointA);
            
            // Then
            assertThat(distanceAtoB)
                .as("Distance should be symmetric")
                .isCloseTo(distanceBtoA, within(0.01));
        }
    }
    
    @Nested
    @DisplayName("Value Object Equality")
    class ValueObjectEquality {
        
        @Test
        void shouldBeEqualWhenCoordinatesMatch() {
            // Given
            var coord1 = new Coordinates(45.4642, 9.1900);
            var coord2 = new Coordinates(45.4642, 9.1900);
            
            // Then
            assertThat(coord1)
                .isEqualTo(coord2)
                .hasSameHashCodeAs(coord2);
        }
        
        @Test
        void shouldNotBeEqualWhenCoordinatesDiffer() {
            // Given
            var milan = new Coordinates(MILAN_LAT, MILAN_LNG);
            var rome = new Coordinates(ROME_LAT, ROME_LNG);
            
            // Then
            assertThat(milan).isNotEqualTo(rome);
        }
    }
}
