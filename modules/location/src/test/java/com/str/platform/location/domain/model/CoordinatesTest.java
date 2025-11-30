package com.str.platform.location.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Coordinates value object.
 */
class CoordinatesTest {
    
    @Test
    @DisplayName("Should create valid coordinates")
    void shouldCreateValidCoordinates() {
        Coordinates coords = new Coordinates(41.9028, 12.4964);
        
        assertThat(coords.getLatitude()).isEqualTo(41.9028);
        assertThat(coords.getLongitude()).isEqualTo(12.4964);
    }
    
    @Test
    @DisplayName("Should reject invalid latitude")
    void shouldRejectInvalidLatitude() {
        assertThatThrownBy(() -> new Coordinates(91.0, 12.4964))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
    }
    
    @Test
    @DisplayName("Should reject invalid longitude")
    void shouldRejectInvalidLongitude() {
        assertThatThrownBy(() -> new Coordinates(41.9028, 181.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
    }
    
    @Test
    @DisplayName("Should calculate distance between Rome and Milan correctly")
    void shouldCalculateDistance() {
        Coordinates rome = new Coordinates(41.9028, 12.4964);
        Coordinates milan = new Coordinates(45.4642, 9.1900);
        
        double distance = rome.distanceTo(milan);
        
        // Approximate distance Rome-Milan is ~480km
        assertThat(distance).isBetween(475.0, 485.0);
    }
    
    @Test
    @DisplayName("Should be equal when coordinates match")
    void shouldBeEqualWhenCoordinatesMatch() {
        Coordinates coord1 = new Coordinates(41.9028, 12.4964);
        Coordinates coord2 = new Coordinates(41.9028, 12.4964);
        
        assertThat(coord1).isEqualTo(coord2);
        assertThat(coord1.hashCode()).isEqualTo(coord2.hashCode());
    }
}
