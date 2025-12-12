package com.str.platform.api.dto;

import com.str.platform.location.domain.model.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for location responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location information")
public class LocationResponse {

    @Schema(description = "Unique location identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Latitude coordinate", example = "41.9028")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "12.4964")
    private Double longitude;

    @Schema(description = "City name", example = "Rome")
    private String city;

    @Schema(description = "Region/state name", example = "Lazio")
    private String region;

    @Schema(description = "Country name", example = "Italy")
    private String country;

    @Schema(description = "Full formatted address", example = "Rome, Lazio, Italy")
    private String fullAddress;

    @Schema(description = "Data quality level", example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
    private String dataQuality;

    @Schema(description = "Number of properties available", example = "150")
    private Integer propertyCount;

    @Schema(description = "Average property price", example = "85.50")
    private Double averagePrice;

    @Schema(description = "Last time data was scraped")
    private Instant lastScraped;

    /**
     * Convert domain Location to DTO
     */
    public static LocationResponse fromDomain(Location location) {
        return LocationResponse.builder()
            .id(location.getId())
            .latitude(location.getCoordinates().getLatitude())
            .longitude(location.getCoordinates().getLongitude())
            .city(location.getAddress().getCity())
            .region(location.getAddress().getRegion())
            .country(location.getAddress().getCountry())
            .fullAddress(location.getAddress().getFullAddress())
            .dataQuality(location.getDataQuality().name())
            .propertyCount(location.getPropertyCount())
            .averagePrice(null)
            .lastScraped(location.getLastScraped() != null 
                ? location.getLastScraped().atZone(java.time.ZoneId.systemDefault()).toInstant()
                : null)
            .build();
    }
}
