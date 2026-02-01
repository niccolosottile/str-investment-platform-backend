package com.str.platform.location.application.service;

import com.str.platform.location.domain.model.Address;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.location.domain.model.Location;
import com.str.platform.location.domain.repository.LocationRepository;
import com.str.platform.location.infrastructure.external.mapbox.MapboxClient;
import com.str.platform.location.infrastructure.external.mapbox.dto.GeocodingResponse;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationService.
 * Tests location search, geocoding integration, and caching behavior.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {
    
    private static final String VALID_QUERY = "Milan, Italy";
    private static final double MILAN_LAT = 45.4642;
    private static final double MILAN_LNG = 9.1900;
    
    @Mock
    private LocationRepository locationRepository;
    
    @Mock
    private MapboxClient mapboxClient;
    
    @InjectMocks
    private LocationService sut;
    
    @Nested
    @DisplayName("Location Search")
    class LocationSearch {
        
        @Test
        void shouldFindBestMatchForValidQuery() {
            // Given
            var response = createGeocodingResponse();
            when(mapboxClient.geocode(VALID_QUERY, 1)).thenReturn(response);
            when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
            
            // When
            Location result = sut.findBestMatch(VALID_QUERY);
            
            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(loc -> {
                    assertThat(loc.getCoordinates().getLatitude()).isCloseTo(MILAN_LAT, within(0.01));
                    assertThat(loc.getCoordinates().getLongitude()).isCloseTo(MILAN_LNG, within(0.01));
                    assertThat(loc.getAddress().getCity()).isEqualTo("Milan");
                });
            
            verify(mapboxClient).geocode(VALID_QUERY, 1);
            verify(locationRepository).save(any(Location.class));
        }
        
        @Test
        void shouldThrowExceptionWhenNoResultsFound() {
            // Given
            when(mapboxClient.geocode(VALID_QUERY, 1)).thenReturn(createEmptyResponse());
            
            // When/Then
            assertThatThrownBy(() -> sut.findBestMatch(VALID_QUERY))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Location")
                .hasMessageContaining(VALID_QUERY);
        }
        
        @Test
        void shouldSearchMultipleLocations() {
            // Given
            int limit = 5;
            var response = createGeocodingResponseWithMultipleResults();
            when(mapboxClient.geocode(VALID_QUERY, limit)).thenReturn(response);
            when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
            
            // When
            List<Location> results = sut.searchLocations(VALID_QUERY, limit);
            
            // Then
            assertThat(results)
                .hasSize(3)
                .allSatisfy(loc -> assertThat(loc.getCoordinates()).isNotNull());
        }
        
        @Test
        void shouldReturnEmptyListWhenSearchFindsNothing() {
            // Given
            when(mapboxClient.geocode(anyString(), anyInt())).thenReturn(createEmptyResponse());
            
            // When
            List<Location> results = sut.searchLocations("NonexistentPlace", 5);
            
            // Then
            assertThat(results).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Nearby Location Search")
    class NearbyLocationSearch {
        
        @Test
        void shouldFindNearbyLocations() {
            // Given
            double radiusKm = 10.0;
            List<Location> nearbyLocations = List.of(
                createLocation("Rome"),
                createLocation("Florence")
            );
            when(locationRepository.findNearby(any(Coordinates.class), eq(radiusKm)))
                .thenReturn(nearbyLocations);
            
            // When
            List<Location> results = sut.findNearby(MILAN_LAT, MILAN_LNG, radiusKm);
            
            // Then
            assertThat(results)
                .hasSize(2)
                .extracting(loc -> loc.getAddress().getCity())
                .containsExactly("Rome", "Florence");
        }
    }
    
    private GeocodingResponse createGeocodingResponse() {
        var feature = new GeocodingResponse.Feature();
        feature.setPlaceName("Milan, Lombardy, Italy");
        feature.setText("Milan");
        feature.setCenter(List.of(MILAN_LNG, MILAN_LAT));
        feature.setContext(createContextList());
        feature.setBbox(List.of(9.04, 45.39, 9.28, 45.54));
        
        var response = new GeocodingResponse();
        response.setFeatures(List.of(feature));
        return response;
    }
    
    private GeocodingResponse createGeocodingResponseWithMultipleResults() {
        var response = createGeocodingResponse();
        response.setFeatures(List.of(
            response.getFeatures().get(0),
            createFeature("Rome", 41.9028, 12.4964),
            createFeature("Florence", 43.7696, 11.2558)
        ));
        return response;
    }
    
    private GeocodingResponse.Feature createFeature(String city, double lat, double lng) {
        var feature = new GeocodingResponse.Feature();
        feature.setPlaceName(city + ", Italy");
        feature.setText(city);
        feature.setCenter(List.of(lng, lat));
        feature.setContext(createContextList());
        return feature;
    }
    
    private List<GeocodingResponse.Context> createContextList() {
        var region = new GeocodingResponse.Context();
        region.setText("Lombardy");
        region.setId("region");
        
        var country = new GeocodingResponse.Context();
        country.setText("Italy");
        country.setId("country");
        country.setShortCode("it");
        
        return List.of(region, country);
    }
    
    private GeocodingResponse createEmptyResponse() {
        var response = new GeocodingResponse();
        response.setFeatures(List.of());
        return response;
    }
    
    private Location createLocation(String city) {
        var coords = new Coordinates(MILAN_LAT, MILAN_LNG);
        var address = new Address(city, "Region", "Italy", null);
        return new Location(coords, address);
    }
}
