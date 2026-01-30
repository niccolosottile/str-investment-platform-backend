package com.str.platform.location.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BoundingBox value object.
 * Tests bounding box creation, validation, and spatial operations.
 */
class BoundingBoxTest {
    
    // Milan area bounding box
    private static final double MILAN_SW_LNG = 9.04;
    private static final double MILAN_SW_LAT = 45.39;
    private static final double MILAN_NE_LNG = 9.28;
    private static final double MILAN_NE_LAT = 45.54;
    
    @Nested
    @DisplayName("BoundingBox Creation")
    class BoundingBoxCreation {
        
        @Test
        void shouldCreateValidBoundingBox() {
            // When
            var bbox = new BoundingBox(MILAN_SW_LNG, MILAN_SW_LAT, MILAN_NE_LNG, MILAN_NE_LAT);
            
            // Then
            assertThat(bbox)
                .satisfies(b -> {
                    assertThat(b.getSouthWestLongitude()).isEqualTo(MILAN_SW_LNG);
                    assertThat(b.getSouthWestLatitude()).isEqualTo(MILAN_SW_LAT);
                    assertThat(b.getNorthEastLongitude()).isEqualTo(MILAN_NE_LNG);
                    assertThat(b.getNorthEastLatitude()).isEqualTo(MILAN_NE_LAT);
                });
        }
        
        @Test
        void shouldRejectWhenSWLongitudeGreaterThanNE() {
            // When/Then
            assertThatThrownBy(() -> new BoundingBox(10.0, 45.0, 9.0, 46.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Southwest longitude must be less than northeast longitude");
        }
        
        @Test
        void shouldRejectWhenSWLatitudeGreaterThanNE() {
            // When/Then
            assertThatThrownBy(() -> new BoundingBox(9.0, 46.0, 10.0, 45.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Southwest latitude must be less than northeast latitude");
        }
        
        @Test
        void shouldRejectInvalidLatitudes() {
            // When/Then
            assertThatThrownBy(() -> new BoundingBox(9.0, -91.0, 10.0, 45.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
            
            assertThatThrownBy(() -> new BoundingBox(9.0, 45.0, 10.0, 91.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
        }
        
        @Test
        void shouldRejectInvalidLongitudes() {
            // When/Then
            assertThatThrownBy(() -> new BoundingBox(-181.0, 45.0, 10.0, 46.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
            
            assertThatThrownBy(() -> new BoundingBox(9.0, 45.0, 181.0, 46.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
        }
    }
    
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {
        
        @Test
        void shouldCreateFromArray() {
            // Given
            double[] bboxArray = {MILAN_SW_LNG, MILAN_SW_LAT, MILAN_NE_LNG, MILAN_NE_LAT};
            
            // When
            var bbox = BoundingBox.fromArray(bboxArray);
            
            // Then
            assertThat(bbox.getSouthWestLongitude()).isEqualTo(MILAN_SW_LNG);
            assertThat(bbox.getNorthEastLatitude()).isEqualTo(MILAN_NE_LAT);
        }
        
        @Test
        void shouldRejectArrayWithWrongSize() {
            // Given
            double[] invalidArray = {9.0, 45.0, 10.0}; // Only 3 elements
            
            // When/Then
            assertThatThrownBy(() -> BoundingBox.fromArray(invalidArray))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have exactly 4 elements");
        }
        
        @Test
        void shouldRejectNullArray() {
            // When/Then
            assertThatThrownBy(() -> BoundingBox.fromArray(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have exactly 4 elements");
        }
    }
    
    
    @Nested
    @DisplayName("Spatial Operations")
    class SpatialOperations {
        
        @Test
        void shouldConfirmCoordinatesInsideBoundingBox() {
            // Given - Milan center coordinates
            var bbox = new BoundingBox(MILAN_SW_LNG, MILAN_SW_LAT, MILAN_NE_LNG, MILAN_NE_LAT);
            var milanCenter = new Coordinates(45.4642, 9.1900);
            
            // When
            boolean contains = bbox.contains(milanCenter);
            
            // Then
            assertThat(contains).isTrue();
        }
        
        @Test
        void shouldConfirmCoordinatesOutsideBoundingBox() {
            // Given
            var bbox = new BoundingBox(MILAN_SW_LNG, MILAN_SW_LAT, MILAN_NE_LNG, MILAN_NE_LAT);
            var rome = new Coordinates(41.9028, 12.4964); // Rome is outside Milan bbox
            
            // When
            boolean contains = bbox.contains(rome);
            
            // Then
            assertThat(contains).isFalse();
        }
        
        @Test
        void shouldCalculateCenterCoordinates() {
            // Given
            var bbox = new BoundingBox(MILAN_SW_LNG, MILAN_SW_LAT, MILAN_NE_LNG, MILAN_NE_LAT);
            
            // When
            var center = bbox.getCenter();
            
            // Then
            double expectedLat = (MILAN_SW_LAT + MILAN_NE_LAT) / 2;
            double expectedLng = (MILAN_SW_LNG + MILAN_NE_LNG) / 2;
            
            assertThat(center.getLatitude()).isCloseTo(expectedLat, within(0.001));
            assertThat(center.getLongitude()).isCloseTo(expectedLng, within(0.001));
        }
        
        @Test
        void shouldCalculateApproximateWidthInKilometers() {
            // Given - A 1-degree wide box at equator (~111km per degree)
            var bbox = new BoundingBox(0.0, 0.0, 1.0, 1.0);
            
            // When
            double widthKm = bbox.getWidthKm();
            
            // Then
            assertThat(widthKm)
                .as("Width should be approximately 111km at equator")
                .isCloseTo(111.0, within(5.0));
        }
        
        @Test
        void shouldCalculateApproximateHeightInKilometers() {
            // Given - A 1-degree tall box
            var bbox = new BoundingBox(0.0, 0.0, 1.0, 1.0);
            
            // When
            double heightKm = bbox.getHeightKm();
            
            // Then
            assertThat(heightKm)
                .as("Height should be approximately 111km per degree of latitude")
                .isCloseTo(111.0, within(5.0));
        }
    }
}
