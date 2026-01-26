package com.str.platform.location.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object representing a geographical bounding box.
 * Used for spatial queries and scraping boundaries.
 * 
 * Format: [minX, minY, maxX, maxY] = [swLng, swLat, neLng, neLat]
 */
@Getter
@EqualsAndHashCode
public class BoundingBox {
    
    private final double southWestLongitude;
    private final double southWestLatitude;
    private final double northEastLongitude;
    private final double northEastLatitude;
    
    /**
     * Create a bounding box from Mapbox format [minX, minY, maxX, maxY]
     */
    public BoundingBox(double swLng, double swLat, double neLng, double neLat) {
        if (swLng > neLng) {
            throw new IllegalArgumentException("Southwest longitude must be less than northeast longitude");
        }
        if (swLat > neLat) {
            throw new IllegalArgumentException("Southwest latitude must be less than northeast latitude");
        }
        if (swLat < -90 || swLat > 90 || neLat < -90 || neLat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (swLng < -180 || swLng > 180 || neLng < -180 || neLng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        
        this.southWestLongitude = swLng;
        this.southWestLatitude = swLat;
        this.northEastLongitude = neLng;
        this.northEastLatitude = neLat;
    }
    
    /**
     * Create bounding box from Mapbox array format
     */
    public static BoundingBox fromArray(double[] bbox) {
        if (bbox == null || bbox.length != 4) {
            throw new IllegalArgumentException("Bounding box array must have exactly 4 elements");
        }
        return new BoundingBox(bbox[0], bbox[1], bbox[2], bbox[3]);
    }
    
    /**
     * Check if coordinates are within this bounding box
     */
    public boolean contains(Coordinates coordinates) {
        return coordinates.getLongitude() >= southWestLongitude
            && coordinates.getLongitude() <= northEastLongitude
            && coordinates.getLatitude() >= southWestLatitude
            && coordinates.getLatitude() <= northEastLatitude;
    }
    
    /**
     * Get center coordinates of the bounding box
     */
    public Coordinates getCenter() {
        double centerLat = (southWestLatitude + northEastLatitude) / 2;
        double centerLng = (southWestLongitude + northEastLongitude) / 2;
        return new Coordinates(centerLat, centerLng);
    }
    
    /**
     * Calculate approximate width in kilometers
     */
    public double getWidthKm() {
        double avgLat = (southWestLatitude + northEastLatitude) / 2;
        return Distance.calculate(
            new Coordinates(avgLat, southWestLongitude),
            new Coordinates(avgLat, northEastLongitude)
        ).getKilometers();
    }
    
    /**
     * Calculate approximate height in kilometers
     */
    public double getHeightKm() {
        double avgLng = (southWestLongitude + northEastLongitude) / 2;
        return Distance.calculate(
            new Coordinates(southWestLatitude, avgLng),
            new Coordinates(northEastLatitude, avgLng)
        ).getKilometers();
    }
    
    @Override
    public String toString() {
        return String.format("BoundingBox[SW(%.4f, %.4f), NE(%.4f, %.4f)]",
            southWestLongitude, southWestLatitude, northEastLongitude, northEastLatitude);
    }
}
