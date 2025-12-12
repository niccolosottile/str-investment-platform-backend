package com.str.platform.location.infrastructure.external.mapbox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for Mapbox Directions API response.
 * Docs: https://docs.mapbox.com/api/navigation/directions/
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectionsResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("routes")
    private List<Route> routes;

    @JsonProperty("waypoints")
    private List<Waypoint> waypoints;

    /**
     * Get the primary route (first route returned)
     */
    public Route getPrimaryRoute() {
        return routes != null && !routes.isEmpty() ? routes.get(0) : null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        
        @JsonProperty("distance")
        private Double distance; // meters

        @JsonProperty("duration")
        private Double duration; // seconds

        @JsonProperty("weight")
        private Double weight;

        @JsonProperty("geometry")
        private String geometry;

        @JsonProperty("legs")
        private List<Leg> legs;

        /**
         * Get distance in kilometers
         */
        public Double getDistanceKm() {
            return distance != null ? distance / 1000.0 : null;
        }

        /**
         * Get duration in minutes
         */
        public Double getDurationMinutes() {
            return duration != null ? duration / 60.0 : null;
        }

        /**
         * Get duration in hours
         */
        public Double getDurationHours() {
            return duration != null ? duration / 3600.0 : null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        
        @JsonProperty("distance")
        private Double distance;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("steps")
        private List<Step> steps;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        
        @JsonProperty("distance")
        private Double distance;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("name")
        private String name;

        @JsonProperty("maneuver")
        private Maneuver maneuver;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Maneuver {
        
        @JsonProperty("type")
        private String type;

        @JsonProperty("instruction")
        private String instruction;

        @JsonProperty("location")
        private List<Double> location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Waypoint {
        
        @JsonProperty("name")
        private String name;

        @JsonProperty("location")
        private List<Double> location; // [longitude, latitude]

        @JsonProperty("distance")
        private Double distance;
    }
}
