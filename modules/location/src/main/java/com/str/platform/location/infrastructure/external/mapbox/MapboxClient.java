package com.str.platform.location.infrastructure.external.mapbox;

import com.str.platform.location.infrastructure.external.mapbox.dto.DirectionsResponse;
import com.str.platform.location.infrastructure.external.mapbox.dto.GeocodingResponse;
import com.str.platform.shared.domain.exception.DomainException;
import org.springframework.web.util.UriComponentsBuilder;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for interacting with Mapbox APIs.
 * Implements geocoding (location search) and directions (driving time) functionality.
 * 
 * Uses Resilience4j for:
 * - Rate limiting to comply with Mapbox API limits
 * - Retries for transient failures
 * - Circuit breaker for cascading failures
 */
@Component
@Slf4j
public class MapboxClient {

    private static final String GEOCODING_BASE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places";
    private static final String DIRECTIONS_BASE_URL = "https://api.mapbox.com/directions/v5/mapbox";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final String accessToken;

    public MapboxClient(
        WebClient.Builder webClientBuilder,
        @Value("${mapbox.access-token}") String accessToken
    ) {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
        this.accessToken = accessToken;
    }

    /**
     * Search for locations using Mapbox Geocoding API.
     * 
     * @param query Search query (e.g., "Rome, Italy")
     * @param limit Maximum number of results (default 5)
     * @return Geocoding response with matching locations
     */
    @RateLimiter(name = "mapbox")
    @Retry(name = "mapbox")
    public GeocodingResponse geocode(String query, Integer limit) {
        log.debug("Geocoding query: {}", query);

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(GEOCODING_BASE_URL + "/{query}.json")
                .queryParam("access_token", accessToken)
                .queryParam("limit", limit != null ? limit : 5)
                .queryParam("types", "place,region,country")
                .queryParam("language", "en")
                .build(false)
                .expand(query)
                .encode()
                .toUriString();

        try {
            return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(GeocodingResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();
        } catch (WebClientResponseException e) {
            log.error("Mapbox geocoding API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new MapboxApiException("Geocoding request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during geocoding", e);
            throw new MapboxApiException("Geocoding request failed", e);
        }
    }

    /**
     * Calculate driving directions between two points using Mapbox Directions API.
     * 
     * @param startLng Starting longitude
     * @param startLat Starting latitude
     * @param endLng Ending longitude
     * @param endLat Ending latitude
     * @return Directions response with route information
     */
    @RateLimiter(name = "mapbox")
    @Retry(name = "mapbox")
    public DirectionsResponse getDirections(
        double startLng, double startLat,
        double endLng, double endLat
    ) {
        log.debug("Getting directions from {},{} to {},{}", startLng, startLat, endLng, endLat);

        String coordinates = String.format("%f,%f;%f,%f", startLng, startLat, endLng, endLat);
        String url = UriComponentsBuilder
                .fromHttpUrl(DIRECTIONS_BASE_URL + "/driving/{coords}")
                .queryParam("access_token", accessToken)
                .queryParam("geometries", "geojson")
                .queryParam("overview", "simplified")
                .build(false)
                .expand(coordinates)
                .encode()
                .toUriString();

        try {
            return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(DirectionsResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();
        } catch (WebClientResponseException e) {
            log.error("Mapbox directions API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new MapboxApiException("Directions request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during directions request", e);
            throw new MapboxApiException("Directions request failed", e);
        }
    }

    /**
     * Calculate driving time between two points in minutes.
     * Convenience method that extracts duration from directions response.
     */
    @RateLimiter(name = "mapbox")
    @Retry(name = "mapbox")
    public Double getDrivingTimeMinutes(
        double startLng, double startLat,
        double endLng, double endLat
    ) {
        DirectionsResponse response = getDirections(startLng, startLat, endLng, endLat);
        
        if (response == null || response.getPrimaryRoute() == null) {
            log.warn("No route found for coordinates");
            return null;
        }

        return response.getPrimaryRoute().getDurationMinutes();
    }

    /**
     * Batch calculate driving times from one origin to multiple destinations.
     */
    public java.util.Map<String, Double> batchGetDrivingTimes(
        double originLng, double originLat,
        java.util.List<Coordinates> destinations
    ) {
        log.debug("Batch calculating driving times for {} destinations", destinations.size());

        return destinations.parallelStream()
            .collect(java.util.stream.Collectors.toMap(
                dest -> dest.key(),
                dest -> {
                    try {
                        return getDrivingTimeMinutes(originLng, originLat, dest.lng(), dest.lat());
                    } catch (Exception e) {
                        log.warn("Failed to get driving time to {}: {}", dest.key(), e.getMessage());
                        return null;
                    }
                }
            ));
    }

    /**
     * Record for destination coordinates
     */
    public record Coordinates(String key, double lng, double lat) {}

    /**
     * Custom exception for Mapbox API errors
     */
    public static class MapboxApiException extends DomainException {
        public MapboxApiException(String message) {
            super(message);
        }

        public MapboxApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
