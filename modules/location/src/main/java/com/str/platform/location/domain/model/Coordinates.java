package com.str.platform.location.domain.model;

import com.str.platform.shared.domain.common.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object representing geographical coordinates.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Coordinates extends ValueObject {
    
    private final double latitude;
    private final double longitude;
    
    public Coordinates(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    private void validateLatitude(double lat) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
    }
    
    private void validateLongitude(double lng) {
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }
    
    /**
     * Calculate distance to another coordinate using Haversine formula.
     * Returns distance in kilometers.
     */
    public double distanceTo(Coordinates other) {
        final int EARTH_RADIUS_KM = 6371;
        
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLng = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}
