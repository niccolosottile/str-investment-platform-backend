package com.str.platform.location.application.service;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Distance;
import com.str.platform.location.infrastructure.external.mapbox.MapboxClient;
import com.str.platform.location.infrastructure.external.mapbox.dto.DirectionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Application service for driving time calculations.
 * Uses Mapbox Directions API with aggressive caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrivingTimeService {

    private final MapboxClient mapboxClient;
    
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Calculate driving time between two points.
     * Results cached for 7 days since driving routes rarely change.
     * 
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @return Distance object with driving time
     */
    @Cacheable(value = "driving-time", key = "#originLat + ',' + #originLng + '-' + #destLat + ',' + #destLng")
    public Distance calculateDrivingTime(
        double originLat, double originLng,
        double destLat, double destLng
    ) {
        log.debug("Calculating driving time from {},{} to {},{}", 
            originLat, originLng, destLat, destLng);

        try {
            DirectionsResponse response = mapboxClient.getDirections(
                originLng, originLat, // Mapbox uses lng,lat
                destLng, destLat
            );

            if (response == null || response.getPrimaryRoute() == null) {
                log.warn("No route found between coordinates");
                // Calculate straight line distance using Haversine formula
                Coordinates origin = new Coordinates(originLat, originLng);
                Coordinates dest = new Coordinates(destLat, destLng);
                double straightLineKm = origin.distanceTo(dest);
                return Distance.fromStraightLine(straightLineKm);
            }

            DirectionsResponse.Route route = response.getPrimaryRoute();
            double distanceKm = route.getDistanceKm();
            double durationMinutes = route.getDurationMinutes();

            log.debug("Route found: {} km, {} minutes", distanceKm, durationMinutes);

            return Distance.fromApi(distanceKm, (int) durationMinutes);

        } catch (Exception e) {
            log.error("Error calculating driving time, falling back to Haversine", e);
            // Calculate straight line distance using Haversine formula
            Coordinates origin = new Coordinates(originLat, originLng);
            Coordinates dest = new Coordinates(destLat, destLng);
            double straightLineKm = origin.distanceTo(dest);
            return Distance.fromStraightLine(straightLineKm);
        }
    }

    /**
     * Calculate driving times from one origin to multiple destinations in parallel.
     * 
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destinations List of destination coordinates
     * @return Map of destination key to driving time distance
     */
    public Map<String, Distance> batchCalculateDrivingTimes(
        double originLat, double originLng,
        List<DestinationCoordinate> destinations
    ) {
        log.info("Batch calculating driving times for {} destinations", destinations.size());

        // Use virtual threads for concurrent API calls
        List<CompletableFuture<DestinationResult>> futures = destinations.stream()
            .map(dest -> CompletableFuture.supplyAsync(() -> {
                try {
                    Distance distance = calculateDrivingTime(
                        originLat, originLng,
                        dest.latitude(), dest.longitude()
                    );
                    return new DestinationResult(dest.key(), distance, null);
                } catch (Exception e) {
                    log.warn("Failed to calculate driving time to {}: {}", dest.key(), e.getMessage());
                    return new DestinationResult(dest.key(), null, e.getMessage());
                }
            }, virtualThreadExecutor))
            .toList();

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        return futures.stream()
            .map(CompletableFuture::join)
            .filter(result -> result.distance() != null)
            .collect(Collectors.toMap(
                DestinationResult::key,
                DestinationResult::distance
            ));
    }

    /**
     * Calculate driving time and return only the duration in minutes.
     * Convenience method for simple time-only queries.
     */
    @Cacheable(value = "driving-time-minutes", key = "#originLat + ',' + #originLng + '-' + #destLat + ',' + #destLng")
    public Double getDrivingTimeMinutes(
        double originLat, double originLng,
        double destLat, double destLng
    ) {
        Distance distance = calculateDrivingTime(originLat, originLng, destLat, destLng);
        Integer minutes = distance.getDrivingTimeMinutes();
        return minutes != null ? minutes.doubleValue() : null;
    }

    /**
     * Check if a destination is within driving time threshold.
     * Useful for filtering opportunities by driving time.
     */
    public boolean isWithinDrivingTime(
        double originLat, double originLng,
        double destLat, double destLng,
        int maxMinutes
    ) {
        try {
            Distance distance = calculateDrivingTime(originLat, originLng, destLat, destLng);
            Integer minutes = distance.getDrivingTimeMinutes();
            return minutes != null && minutes <= maxMinutes;
        } catch (Exception e) {
            log.error("Error checking driving time threshold", e);
            return false;
        }
    }

    /**
     * Record for destination coordinates
     */
    public record DestinationCoordinate(String key, double latitude, double longitude) {}

    /**
     * Record for batch calculation result
     */
    private record DestinationResult(String key, Distance distance, String error) {}
}
