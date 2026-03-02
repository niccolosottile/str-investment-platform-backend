package com.str.platform.location.infrastructure.external.mapbox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for Mapbox Search Geocoding API v6 response.
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

        @JsonProperty("geometry")
        private Geometry geometry;

        @JsonProperty("properties")
        private Properties properties;

        public Double getLatitude() {
            if (geometry != null && geometry.getCoordinates() != null && geometry.getCoordinates().size() > 1) {
                return geometry.getCoordinates().get(1);
            }
            if (properties != null && properties.getCoordinates() != null) {
                return properties.getCoordinates().getLatitude();
            }
            return null;
        }

        public Double getLongitude() {
            if (geometry != null && geometry.getCoordinates() != null && !geometry.getCoordinates().isEmpty()) {
                return geometry.getCoordinates().get(0);
            }
            if (properties != null && properties.getCoordinates() != null) {
                return properties.getCoordinates().getLongitude();
            }
            return null;
        }

        public String getCity() {
            if (properties == null) return null;
            // For a place/locality feature, the name itself is the city
            String featureType = properties.getFeatureType();
            if ("place".equals(featureType) || "locality".equals(featureType)) {
                return properties.getName();
            }
            // Otherwise pull from context
            if (properties.getContext() != null && properties.getContext().getPlace() != null) {
                return properties.getContext().getPlace().getName();
            }
            return properties.getName();
        }

        public String getRegion() {
            if (properties == null || properties.getContext() == null) return null;
            ContextObject ctx = properties.getContext();
            // Prefer the province-level region (district), fall back to region
            if (ctx.getDistrict() != null) return ctx.getDistrict().getName();
            if (ctx.getRegion() != null) return ctx.getRegion().getName();
            return null;
        }

        public String getCountry() {
            if (properties == null || properties.getContext() == null) return null;
            if (properties.getContext().getCountry() != null) {
                return properties.getContext().getCountry().getName();
            }
            return null;
        }

        public String getPlaceName() {
            return properties != null ? properties.getFullAddress() : null;
        }

        // Keep getText() for compatibility with LocationService
        public String getText() {
            return properties != null ? properties.getName() : null;
        }

        public boolean hasBoundingBox() {
            return properties != null && properties.getBbox() != null && properties.getBbox().size() == 4;
        }

        public double[] getBoundingBox() {
            if (!hasBoundingBox()) return null;
            List<Double> b = properties.getBbox();
            return new double[]{b.get(0), b.get(1), b.get(2), b.get(3)};
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
    public static class Properties {
        @JsonProperty("mapbox_id")
        private String mapboxId;
        @JsonProperty("feature_type")
        private String featureType;
        @JsonProperty("full_address")
        private String fullAddress;
        @JsonProperty("name")
        private String name;
        @JsonProperty("coordinates")
        private CoordinatesObj coordinates;
        @JsonProperty("bbox")
        private List<Double> bbox;
        @JsonProperty("context")
        private ContextObject context;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoordinatesObj {
        @JsonProperty("longitude")
        private Double longitude;
        @JsonProperty("latitude")
        private Double latitude;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContextObject {
        @JsonProperty("place")
        private ContextEntry place;
        @JsonProperty("locality")
        private ContextEntry locality;
        @JsonProperty("district")
        private ContextEntry district;
        @JsonProperty("region")
        private ContextEntry region;
        @JsonProperty("country")
        private ContextEntry country;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContextEntry {
        @JsonProperty("mapbox_id")
        private String mapboxId;
        @JsonProperty("name")
        private String name;
    }
}
