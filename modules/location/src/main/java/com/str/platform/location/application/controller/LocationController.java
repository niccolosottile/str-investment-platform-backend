package com.str.platform.location.application.controller;

import com.str.platform.location.application.dto.*;
import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.Location;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for location-related operations.
 * Provides endpoints for location search, nearby search, and location details.
 */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Locations", description = "Location search and management APIs")
public class LocationController {

    private final LocationService locationService;

    @Operation(
        summary = "Search locations",
        description = "Search for locations by query string (city, region, or country)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful search"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @GetMapping("/search")
    public ResponseEntity<List<LocationResponse>> searchLocations(
        @Parameter(description = "Search query", example = "Rome, Italy")
        @RequestParam String query,
        
        @Parameter(description = "Maximum number of results", example = "5")
        @RequestParam(defaultValue = "5") 
        @Min(1) @Max(10) 
        Integer limit
    ) {
        log.info("Searching locations: query={}, limit={}", query, limit);

        List<Location> locations = locationService.searchLocations(query, limit);
        List<LocationResponse> response = locations.stream()
            .map(LocationResponse::fromDomain)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Find nearby locations",
        description = "Find locations within a specified radius from a given point"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful search"),
        @ApiResponse(responseCode = "400", description = "Invalid coordinates or radius")
    })
    @GetMapping("/nearby")
    public ResponseEntity<List<LocationResponse>> findNearby(
        @Parameter(description = "Latitude", example = "41.9028")
        @RequestParam 
        @Min(-90) @Max(90) 
        Double lat,
        
        @Parameter(description = "Longitude", example = "12.4964")
        @RequestParam 
        @Min(-180) @Max(180) 
        Double lng,
        
        @Parameter(description = "Search radius in kilometers", example = "50")
        @RequestParam(defaultValue = "50") 
        @Min(1) @Max(500) 
        Double radius
    ) {
        log.info("Finding nearby locations: lat={}, lng={}, radius={}", lat, lng, radius);

        List<Location> locations = locationService.findNearby(lat, lng, radius);
        List<LocationResponse> response = locations.stream()
            .map(LocationResponse::fromDomain)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get location by ID",
        description = "Retrieve detailed information about a specific location"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Location found",
            content = @Content(schema = @Schema(implementation = LocationResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocationById(
        @Parameter(description = "Location ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id
    ) {
        log.info("Getting location by ID: {}", id);

        Location location = locationService.getById(id);
        LocationResponse response = LocationResponse.fromDomain(location);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get all locations",
        description = "Retrieve all locations in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful retrieval")
    })
    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAllLocations() {
        log.info("Getting all locations");

        List<Location> locations = locationService.getAllLocations();
        List<LocationResponse> response = locations.stream()
            .map(LocationResponse::fromDomain)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete location",
        description = "Delete a location by ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Location deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(
        @Parameter(description = "Location ID")
        @PathVariable UUID id
    ) {
        log.info("Deleting location: {}", id);

        locationService.deleteLocation(id);

        return ResponseEntity.noContent().build();
    }
}
