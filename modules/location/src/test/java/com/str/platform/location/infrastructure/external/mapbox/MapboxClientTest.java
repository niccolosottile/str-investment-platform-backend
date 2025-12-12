package com.str.platform.location.infrastructure.external.mapbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.str.platform.location.infrastructure.external.mapbox.dto.DirectionsResponse;
import com.str.platform.location.infrastructure.external.mapbox.dto.GeocodingResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for MapboxClient using MockWebServer.
 * Tests API integration without hitting actual Mapbox endpoints.
 */
class MapboxClientTest {

    private MockWebServer mockWebServer;
    private MapboxClient mapboxClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(baseUrl);
        
        mapboxClient = new MapboxClient(webClientBuilder, "test-token");
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldGeocodeLocation() throws Exception {
        // Given
        GeocodingResponse mockResponse = createMockGeocodingResponse();
        String jsonResponse = objectMapper.writeValueAsString(mockResponse);

        mockWebServer.enqueue(new MockResponse()
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        GeocodingResponse response = mapboxClient.geocode("Rome, Italy", 5);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFeatures()).hasSize(1);
        
        GeocodingResponse.Feature feature = response.getFeatures().get(0);
        assertThat(feature.getPlaceName()).contains("Rome");
        assertThat(feature.getLatitude()).isCloseTo(41.9028, within(0.001));
        assertThat(feature.getLongitude()).isCloseTo(12.4964, within(0.001));
    }

    @Test
    void shouldThrowExceptionForEmptyQuery() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> 
            mapboxClient.geocode("", 5)
        );
    }

    @Test
    void shouldGetDirections() throws Exception {
        // Given
        DirectionsResponse mockResponse = createMockDirectionsResponse();
        String jsonResponse = objectMapper.writeValueAsString(mockResponse);

        mockWebServer.enqueue(new MockResponse()
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        DirectionsResponse response = mapboxClient.getDirections(
            12.4964, 41.9028,  // Rome
            9.1900, 45.4642    // Milan
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("Ok");
        assertThat(response.getPrimaryRoute()).isNotNull();
        assertThat(response.getPrimaryRoute().getDurationMinutes()).isGreaterThan(0);
    }

    @Test
    void shouldCalculateDrivingTimeInMinutes() throws Exception {
        // Given
        DirectionsResponse mockResponse = createMockDirectionsResponse();
        String jsonResponse = objectMapper.writeValueAsString(mockResponse);

        mockWebServer.enqueue(new MockResponse()
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"));

        // When
        Double drivingTime = mapboxClient.getDrivingTimeMinutes(
            12.4964, 41.9028,
            9.1900, 45.4642
        );

        // Then
        assertThat(drivingTime).isNotNull();
        assertThat(drivingTime).isGreaterThan(0);
    }

    @Test
    void shouldHandleApiError() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody("{\"message\":\"Invalid token\"}"));

        // When/Then
        assertThrows(MapboxClient.MapboxApiException.class, () ->
            mapboxClient.geocode("Rome", 5)
        );
    }

    @Test
    void shouldExtractCityRegionCountryFromGeocodingResponse() {
        // Given
        GeocodingResponse.Feature feature = new GeocodingResponse.Feature();
        feature.setText("Rome");
        feature.setPlaceType(List.of("place"));
        feature.setCenter(List.of(12.4964, 41.9028));

        GeocodingResponse.Context regionContext = new GeocodingResponse.Context();
        regionContext.setId("region.123");
        regionContext.setText("Lazio");

        GeocodingResponse.Context countryContext = new GeocodingResponse.Context();
        countryContext.setId("country.456");
        countryContext.setText("Italy");

        feature.setContext(List.of(regionContext, countryContext));

        // When/Then
        assertThat(feature.getCity()).isEqualTo("Rome");
        assertThat(feature.getRegion()).isEqualTo("Lazio");
        assertThat(feature.getCountry()).isEqualTo("Italy");
        assertThat(feature.getLatitude()).isEqualTo(41.9028);
        assertThat(feature.getLongitude()).isEqualTo(12.4964);
    }

    // Helper methods to create mock responses

    private GeocodingResponse createMockGeocodingResponse() {
        GeocodingResponse response = new GeocodingResponse();
        response.setType("FeatureCollection");

        GeocodingResponse.Feature feature = new GeocodingResponse.Feature();
        feature.setId("place.123");
        feature.setType("Feature");
        feature.setPlaceType(List.of("place"));
        feature.setRelevance(1.0);
        feature.setPlaceName("Rome, Lazio, Italy");
        feature.setText("Rome");
        feature.setCenter(List.of(12.4964, 41.9028));

        GeocodingResponse.Geometry geometry = new GeocodingResponse.Geometry();
        geometry.setType("Point");
        geometry.setCoordinates(List.of(12.4964, 41.9028));
        feature.setGeometry(geometry);

        response.setFeatures(List.of(feature));
        return response;
    }

    private DirectionsResponse createMockDirectionsResponse() {
        DirectionsResponse response = new DirectionsResponse();
        response.setCode("Ok");

        DirectionsResponse.Route route = new DirectionsResponse.Route();
        route.setDistance(574000.0); // 574 km
        route.setDuration(21600.0);  // 360 minutes (6 hours)
        route.setGeometry("mock_geometry_string");

        response.setRoutes(List.of(route));
        return response;
    }

    private static org.assertj.core.data.Offset<Double> within(double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}
