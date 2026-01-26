package com.str.platform.location.infrastructure.external.mapbox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for Mapbox Geocoding API response.
 * Docs: https://docs.mapbox.com/api/search/geocoding/
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("features")
    private List<Feature> features;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        
        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("place_type")
        private List<String> placeType;

        @JsonProperty("relevance")
        private Double relevance;

        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("text")
        private String text;

        @JsonProperty("center")
        private List<Double> center; // [longitude, latitude]

        @JsonProperty("geometry")
        private Geometry geometry;

        @JsonProperty("context")
        private List<Context> context;
        
        @JsonProperty("bbox")
        private List<Double> bbox; // [minX, minY, maxX, maxY]

        /**
         * Get latitude from center coordinates
         */
        public Double getLatitude() {
            return center != null && center.size() > 1 ? center.get(1) : null;
        }

        /**
         * Get longitude from center coordinates
         */
        public Double getLongitude() {
            return center != null && !center.isEmpty() ? center.get(0) : null;
        }

        /**
         * Extract city from context
         */
        public String getCity() {
            if (placeType != null && placeType.contains("place")) {
                return text;
            }
            return findContextValue("place");
        }

        /**
         * Extract region from context
         */
        public String getRegion() {
            return findContextValue("region");
        }

        /**
         * Extract country from context
         */
        public String getCountry() {
            return findContextValue("country");
        }
        
        /**
         * Check if bounding box is available
         */
        public boolean hasBoundingBox() {
            return bbox != null && bbox.size() == 4;
        }
        
        /**
         * Get bounding box as array [minX, minY, maxX, maxY]
         */
        public double[] getBoundingBox() {
            if (!hasBoundingBox()) {
                return null;
            }
            return new double[] {bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3)};
        }

        private String findContextValue(String type) {
            if (context == null) return null;
            return context.stream()
                .filter(c -> c.getId() != null && c.getId().startsWith(type))
                .map(Context::getText)
                .findFirst()
                .orElse(null);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        
        @JsonProperty("type")
        private String type;

        @JsonProperty("coordinates")
        private List<Double> coordinates; // [longitude, latitude]
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Context {
        
        @JsonProperty("id")
        private String id;

        @JsonProperty("text")
        private String text;

        @JsonProperty("short_code")
        private String shortCode;
    }
}
