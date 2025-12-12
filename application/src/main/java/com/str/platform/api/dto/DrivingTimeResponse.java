package com.str.platform.api.dto;

import com.str.platform.location.domain.model.Distance;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for driving time responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Driving time and distance information")
public class DrivingTimeResponse {

    @Schema(description = "Distance in kilometers", example = "574.2")
    private Double distanceKm;

    @Schema(description = "Driving time in minutes", example = "360")
    private Long drivingTimeMinutes;

    @Schema(description = "Driving time in hours", example = "6.0")
    private Double drivingTimeHours;

    @Schema(description = "Whether driving time was calculated from API or estimated", example = "true")
    private Boolean calculated;

    /**
     * Convert domain Distance to DTO
     */
    public static DrivingTimeResponse fromDomain(Distance distance) {
        return DrivingTimeResponse.builder()
            .distanceKm(distance.getKilometers())
            .drivingTimeMinutes(distance.getDrivingTimeMinutes() != null ? distance.getDrivingTimeMinutes().longValue() : null)
            .drivingTimeHours(distance.getDrivingTimeMinutes() != null ? distance.getDrivingTimeMinutes() / 60.0 : null)
            .calculated(distance.getDrivingTimeMinutes() != null)
            .build();
    }
}
