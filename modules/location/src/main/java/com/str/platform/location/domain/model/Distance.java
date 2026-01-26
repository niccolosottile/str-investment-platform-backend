package com.str.platform.location.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object representing distance and driving time between two locations.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Distance {
    
    private final double kilometers;
    private final Integer drivingTimeMinutes;
    private final DrivingTimeSource source;
    
    public enum DrivingTimeSource {
        API,        // From Mapbox Directions API
        HEURISTIC   // Calculated estimate
    }
    
    public Distance(double kilometers, Integer drivingTimeMinutes, DrivingTimeSource source) {
        if (kilometers < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
        if (drivingTimeMinutes != null && drivingTimeMinutes < 0) {
            throw new IllegalArgumentException("Driving time cannot be negative");
        }
        
        this.kilometers = kilometers;
        this.drivingTimeMinutes = drivingTimeMinutes;
        this.source = source != null ? source : DrivingTimeSource.HEURISTIC;
    }
    
    /**
     * Create distance with only straight-line distance (no driving time).
     */
    public static Distance fromStraightLine(double kilometers) {
        return new Distance(kilometers, null, DrivingTimeSource.HEURISTIC);
    }
    
    /**
     * Create distance with API-calculated driving time.
     */
    public static Distance fromApi(double kilometers, int drivingTimeMinutes) {
        return new Distance(kilometers, drivingTimeMinutes, DrivingTimeSource.API);
    }
    
    /**
     * Calculate straight-line distance between two coordinates using Haversine formula.
     */
    public static Distance calculate(Coordinates from, Coordinates to) {
        final int EARTH_RADIUS_KM = 6371;
        
        double lat1Rad = Math.toRadians(from.getLatitude());
        double lat2Rad = Math.toRadians(to.getLatitude());
        double deltaLat = Math.toRadians(to.getLatitude() - from.getLatitude());
        double deltaLng = Math.toRadians(to.getLongitude() - from.getLongitude());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double kilometers = EARTH_RADIUS_KM * c;
        
        return fromStraightLine(kilometers);
    }
}
