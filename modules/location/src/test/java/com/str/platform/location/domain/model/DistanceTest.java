package com.str.platform.location.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Distance value object.
 * Tests distance creation, validation, and static factory methods.
 */
class DistanceTest {
    
    private static final double VALID_DISTANCE_KM = 50.0;
    private static final int VALID_DRIVING_TIME = 45;
    
    @Nested
    @DisplayName("Distance Creation")
    class DistanceCreation {
        
        @Test
        void shouldCreateDistanceWithAllParameters() {
            // When
            var distance = new Distance(VALID_DISTANCE_KM, VALID_DRIVING_TIME, Distance.DrivingTimeSource.API);
            
            // Then
            assertThat(distance)
                .satisfies(d -> {
                    assertThat(d.getKilometers()).isEqualTo(VALID_DISTANCE_KM);
                    assertThat(d.getDrivingTimeMinutes()).isEqualTo(VALID_DRIVING_TIME);
                    assertThat(d.getSource()).isEqualTo(Distance.DrivingTimeSource.API);
                });
        }
        
        @Test
        void shouldRejectNegativeDistance() {
            // When/Then
            assertThatThrownBy(() -> new Distance(-10.0, VALID_DRIVING_TIME, Distance.DrivingTimeSource.API))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distance cannot be negative");
        }
        
        @Test
        void shouldRejectNegativeDrivingTime() {
            // When/Then
            assertThatThrownBy(() -> new Distance(VALID_DISTANCE_KM, -10, Distance.DrivingTimeSource.API))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Driving time cannot be negative");
        }
        
        @Test
        void shouldAcceptNullDrivingTime() {
            // When
            var distance = new Distance(VALID_DISTANCE_KM, null, Distance.DrivingTimeSource.HEURISTIC);
            
            // Then
            assertThat(distance.getDrivingTimeMinutes()).isNull();
        }
        
        @Test
        void shouldDefaultToHeuristicWhenSourceIsNull() {
            // When
            var distance = new Distance(VALID_DISTANCE_KM, VALID_DRIVING_TIME, null);
            
            // Then
            assertThat(distance.getSource()).isEqualTo(Distance.DrivingTimeSource.HEURISTIC);
        }
    }
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        void shouldCreateFromStraightLineDistance() {
            // When
            var distance = Distance.fromStraightLine(100.5);
            
            // Then
            assertThat(distance)
                .satisfies(d -> {
                    assertThat(d.getKilometers()).isEqualTo(100.5);
                    assertThat(d.getDrivingTimeMinutes()).isNull();
                    assertThat(d.getSource()).isEqualTo(Distance.DrivingTimeSource.HEURISTIC);
                });
        }
        
        @Test
        void shouldCreateFromApiWithDrivingTime() {
            // When
            var distance = Distance.fromApi(75.3, 90);
            
            // Then
            assertThat(distance)
                .satisfies(d -> {
                    assertThat(d.getKilometers()).isEqualTo(75.3);
                    assertThat(d.getDrivingTimeMinutes()).isEqualTo(90);
                    assertThat(d.getSource()).isEqualTo(Distance.DrivingTimeSource.API);
                });
        }
    }
    
    @Nested
    @DisplayName("Distance Calculation")
    class DistanceCalculation {
        
        @Test
        void shouldCalculateDistanceBetweenCoordinates() {
            // Given - Milan and Rome coordinates
            var milan = new Coordinates(45.4642, 9.1900);
            var rome = new Coordinates(41.9028, 12.4964);
            
            // When
            var distance = Distance.calculate(milan, rome);
            
            // Then
            assertThat(distance.getKilometers())
                .as("Straight-line distance from Milan to Rome")
                .isCloseTo(478.0, within(10.0)); // Allow 10km tolerance for Haversine approximation
            assertThat(distance.getDrivingTimeMinutes()).isNull();
            assertThat(distance.getSource()).isEqualTo(Distance.DrivingTimeSource.HEURISTIC);
        }
        
        @ParameterizedTest(name = "Distance between ({0},{1}) and ({2},{3})")
        @CsvSource({
            "0, 0, 0, 0, 0",          // Same point
            "0, 0, 1, 0, 111.19",     // ~111km per degree latitude
            "45, 9, 45, 10, 78.7",    // 1 degree longitude at 45° latitude
        })
        void shouldCalculateExpectedDistances(double lat1, double lng1, double lat2, double lng2, double expectedKm) {
            // Given
            var from = new Coordinates(lat1, lng1);
            var to = new Coordinates(lat2, lng2);
            
            // When
            var distance = Distance.calculate(from, to);
            
            // Then
            assertThat(distance.getKilometers())
                .isCloseTo(expectedKm, within(2.0)); // Allow 2km margin
        }
    }
    
    @Nested
    @DisplayName("Value Object Equality")
    class ValueObjectEquality {
        
        @Test
        void shouldBeEqualWhenAllPropertiesMatch() {
            // Given
            var distance1 = new Distance(50.0, 45, Distance.DrivingTimeSource.API);
            var distance2 = new Distance(50.0, 45, Distance.DrivingTimeSource.API);
            
            // Then
            assertThat(distance1)
                .isEqualTo(distance2)
                .hasSameHashCodeAs(distance2);
        }
        
        @Test
        void shouldNotBeEqualWhenDistanceDiffers() {
            // Given
            var distance1 = new Distance(50.0, 45, Distance.DrivingTimeSource.API);
            var distance2 = new Distance(60.0, 45, Distance.DrivingTimeSource.API);
            
            // Then
            assertThat(distance1).isNotEqualTo(distance2);
        }
    }
}
