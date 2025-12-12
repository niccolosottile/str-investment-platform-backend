package com.str.platform.api;

import com.str.platform.api.dto.DrivingTimeRequest;
import com.str.platform.api.dto.DrivingTimeResponse;
import com.str.platform.location.application.service.DrivingTimeService;
import com.str.platform.location.domain.model.Distance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for driving time calculations.
 * Provides endpoint for calculating driving time and distance between two points.
 */
@RestController
@RequestMapping("/api/driving-time")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Driving Time", description = "Driving time and distance calculation APIs")
public class DrivingTimeController {

    private final DrivingTimeService drivingTimeService;

    @Operation(
        summary = "Calculate driving time",
        description = "Calculate driving time and distance between two geographic points using Mapbox Directions API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Driving time calculated successfully",
            content = @Content(schema = @Schema(implementation = DrivingTimeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Error calculating driving time")
    })
    @PostMapping
    public ResponseEntity<DrivingTimeResponse> calculateDrivingTime(
        @Valid @RequestBody DrivingTimeRequest request
    ) {
        log.info("Calculating driving time from {},{} to {},{}",
            request.getOriginLatitude(), request.getOriginLongitude(),
            request.getDestinationLatitude(), request.getDestinationLongitude());

        Distance distance = drivingTimeService.calculateDrivingTime(
            request.getOriginLatitude(),
            request.getOriginLongitude(),
            request.getDestinationLatitude(),
            request.getDestinationLongitude()
        );

        DrivingTimeResponse response = DrivingTimeResponse.fromDomain(distance);

        return ResponseEntity.ok(response);
    }
}
