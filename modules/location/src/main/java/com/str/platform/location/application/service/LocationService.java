package com.str.platform.location.application.service;

import com.str.platform.location.domain.model.Address;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;
import com.str.platform.location.domain.repository.LocationRepository;
import com.str.platform.location.infrastructure.external.mapbox.MapboxClient;
import com.str.platform.location.infrastructure.external.mapbox.dto.GeocodingResponse;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for location operations.
 * Orchestrates domain logic, repository access, and external API calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final MapboxClient mapboxClient;

    /**
     * Search for locations by query string.
     * Uses Mapbox Geocoding API and caches results for 1 hour.
     * 
     * @param query Search query (e.g., "Rome, Italy")
     * @param limit Maximum number of results
     * @return List of matching locations
     */
    @Cacheable(value = "location-search", key = "#query + '-' + #limit")
    @Transactional
    public List<Location> searchLocations(String query, Integer limit) {
        log.info("Searching locations for query: {}", query);

        // Call Mapbox Geocoding API
        GeocodingResponse response = mapboxClient.geocode(query, limit != null ? limit : 5);

        if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
            log.warn("No locations found for query: {}", query);
            return List.of();
        }

        // Convert geocoding results to domain locations and save
        return response.getFeatures().stream()
            .map(this::convertAndSaveLocation)
            .collect(Collectors.toList());
    }

    /**
     * Find nearby locations within a radius.
     * Returns cached results if available.
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusKm Search radius in kilometers
     * @return List of nearby locations
     */
    @Cacheable(value = "nearby-opportunities", key = "#latitude + ',' + #longitude + '-' + #radiusKm")
    @Transactional(readOnly = true)
    public List<Location> findNearby(double latitude, double longitude, double radiusKm) {
        log.info("Finding locations within {} km of {}, {}", radiusKm, latitude, longitude);
        
        List<Location> nearby = locationRepository.findNearby(latitude, longitude, radiusKm);
        
        log.info("Found {} locations nearby", nearby.size());
        return nearby;
    }

    /**
     * Get location by ID.
     * 
     * @param id Location ID
     * @return Location if found
     * @throws EntityNotFoundException if location not found
     */
    @Transactional(readOnly = true)
    public Location getById(UUID id) {
        log.debug("Getting location by ID: {}", id);
        
        return locationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Location not found with ID: " + id));
    }

    /**
     * Find or create location by coordinates.
     * If location exists in database, return it. Otherwise, reverse geocode and save.
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Existing or newly created location
     */
    @Transactional
    public Location findOrCreateByCoordinates(double latitude, double longitude) {
        log.debug("Finding or creating location at {}, {}", latitude, longitude);

        // Check if location already exists
        Optional<Location> existing = locationRepository.findByCoordinates(latitude, longitude);
        if (existing.isPresent()) {
            log.debug("Location already exists in database");
            return existing.get();
        }

        // Reverse geocode to get address information
        String query = String.format("%f,%f", longitude, latitude); // Note: Mapbox uses lng,lat
        GeocodingResponse response = mapboxClient.geocode(query, 1);

        if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
            log.warn("Could not reverse geocode coordinates {}, {}", latitude, longitude);
            // Create location with minimal info
            Coordinates coords = new Coordinates(latitude, longitude);
            Address address = new Address("Unknown", null, "Unknown");
            Location location = new Location(null, coords, address);
            return locationRepository.save(location);
        }

        // Convert and save location
        return convertAndSaveLocation(response.getFeatures().get(0));
    }

    /**
     * Update location with scraped data.
     * 
     * @param locationId Location ID
     * @param propertyCount Number of properties found
     * @param averagePrice Average property price
     * @return Updated location
     */
    @Transactional
    public Location updateScrapedData(UUID locationId, int propertyCount, double averagePrice) {
        log.info("Updating scraped data for location {}: {} properties, avg price {}", 
            locationId, propertyCount, averagePrice);

        Location location = getById(locationId);
        location.updateScrapedData(propertyCount, averagePrice);
        
        return locationRepository.save(location);
    }

    /**
     * Get all locations.
     * 
     * @return List of all locations
     */
    @Transactional(readOnly = true)
    public List<Location> getAllLocations() {
        log.debug("Getting all locations");
        return locationRepository.findAll();
    }

    /**
     * Delete location by ID.
     * 
     * @param id Location ID
     */
    @Transactional
    public void deleteLocation(UUID id) {
        log.info("Deleting location: {}", id);
        
        // Verify location exists
        getById(id);
        
        locationRepository.delete(id);
    }

    /**
     * Convert Mapbox geocoding feature to domain location and save to database.
     */
    private Location convertAndSaveLocation(GeocodingResponse.Feature feature) {
        Coordinates coordinates = new Coordinates(
            feature.getLatitude(),
            feature.getLongitude()
        );

        Address address = new Address(
            feature.getCity() != null ? feature.getCity() : feature.getText(),
            feature.getRegion(),
            feature.getCountry() != null ? feature.getCountry() : "Unknown"
        );

        // Check if location already exists by coordinates
        Optional<Location> existing = locationRepository.findByCoordinates(
            coordinates.latitude(),
            coordinates.longitude()
        );

        if (existing.isPresent()) {
            log.debug("Location already exists, returning existing");
            return existing.get();
        }

        // Create and save new location
        Location location = new Location(null, coordinates, address);
        return locationRepository.save(location);
    }
}
