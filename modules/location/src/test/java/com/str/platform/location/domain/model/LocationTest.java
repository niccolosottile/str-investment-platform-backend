package com.str.platform.location.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Location aggregate root.
 * Tests business logic around data quality calculation and refresh requirements.
 */
class LocationTest {
    
    private static final double MILAN_LAT = 45.4642;
    private static final double MILAN_LNG = 9.1900;
    
    @Nested
    @DisplayName("Location Creation")
    class LocationCreation {
        
        @Test
        void shouldCreateLocationWithMinimalData() {
            // Given
            var coordinates = new Coordinates(MILAN_LAT, MILAN_LNG);
            var address = new Address("Milan", "Lombardy", "Italy", null);
            
            // When
            var location = new Location(coordinates, address);
            
            // Then
            assertThat(location)
                .satisfies(loc -> {
                    assertThat(loc.getCoordinates()).isEqualTo(coordinates);
                    assertThat(loc.getAddress()).isEqualTo(address);
                    assertThat(loc.getDataQuality()).isEqualTo(Location.DataQuality.LOW);
                    assertThat(loc.getPropertyCount()).isEqualTo(0);
                    assertThat(loc.getLastScraped()).isNull();
                });
        }
        
        @Test
        void shouldCreateLocationWithBoundingBox() {
            // Given
            var coordinates = new Coordinates(MILAN_LAT, MILAN_LNG);
            var address = new Address("Milan", "Lombardy", "Italy", null);
            var bbox = new BoundingBox(9.04, 45.39, 9.28, 45.54);
            
            // When
            var location = new Location(coordinates, address, bbox);
            
            // Then
            assertThat(location.getBoundingBox()).isEqualTo(bbox);
        }
        
        @Test
        void shouldHaveLocationNameFromAddress() {
            // Given
            var coordinates = new Coordinates(MILAN_LAT, MILAN_LNG);
            var address = new Address("Milan", "Lombardy", "Italy", null);
            var location = new Location(coordinates, address);
            
            // When
            String name = location.getName();
            
            // Then
            assertThat(name).isEqualTo("Milan, Lombardy, Italy");
        }
    }
        
    @Nested
    @DisplayName("Data Quality Calculation")
    class DataQualityCalculation {
        
        private Location location;
        
        @BeforeEach
        void setUp() {
            var coordinates = new Coordinates(MILAN_LAT, MILAN_LNG);
            var address = new Address("Milan", "Lombardy", "Italy", null);
            location = new Location(coordinates, address);
        }
        
        @Test
        void shouldCalculateHighQualityWithRecentDataAndManyProperties() {
            // Given - High quality: ≥50 properties, scraped <24h ago
            int propertyCount = 75;
            LocalDateTime scrapedAt = LocalDateTime.now().minusHours(12);
            
            // When
            location.updateScrapingData(propertyCount, scrapedAt);
            
            // Then
            assertThat(location.getDataQuality()).isEqualTo(Location.DataQuality.HIGH);
            assertThat(location.getPropertyCount()).isEqualTo(75);
            assertThat(location.getLastScraped()).isEqualTo(scrapedAt);
        }
        
        @Test
        void shouldCalculateMediumQualityWithModerateData() {
            // Given - Medium quality: 10-50 properties, scraped <7d ago
            int propertyCount = 25;
            LocalDateTime scrapedAt = LocalDateTime.now().minusDays(3);
            
            // When
            location.updateScrapingData(propertyCount, scrapedAt);
            
            // Then
            assertThat(location.getDataQuality()).isEqualTo(Location.DataQuality.MEDIUM);
        }
        
        @Test
        void shouldCalculateLowQualityWithFewProperties() {
            // Given - Low quality: <10 properties
            int propertyCount = 5;
            LocalDateTime scrapedAt = LocalDateTime.now().minusHours(1);
            
            // When
            location.updateScrapingData(propertyCount, scrapedAt);
            
            // Then
            assertThat(location.getDataQuality()).isEqualTo(Location.DataQuality.LOW);
        }
        
        @Test
        void shouldCalculateLowQualityWithStaleData() {
            // Given - Low quality: old data (>7 days)
            int propertyCount = 60;
            LocalDateTime scrapedAt = LocalDateTime.now().minusDays(8);
            
            // When
            location.updateScrapingData(propertyCount, scrapedAt);
            
            // Then
            assertThat(location.getDataQuality())
                .as("Even with 60 properties, stale data (>7d) should be LOW quality")
                .isEqualTo(Location.DataQuality.LOW);
        }
        
        @Test
        void shouldCalculateLowQualityWhenScrapedAtIsNull() {
            // When
            location.updateScrapingData(100, null);
            
            // Then
            assertThat(location.getDataQuality()).isEqualTo(Location.DataQuality.LOW);
        }
        
        @Test
        void shouldNotAchieveHighQualityWithSlightlyOldData() {
            // Given - 50 properties but scraped 25 hours ago (just over threshold)
            int propertyCount = 50;
            LocalDateTime scrapedAt = LocalDateTime.now().minusHours(25);
            
            // When
            location.updateScrapingData(propertyCount, scrapedAt);
            
            // Then
            assertThat(location.getDataQuality())
                .as("Data older than 24h should not be HIGH quality")
                .isNotEqualTo(Location.DataQuality.HIGH);
        }
    }
    
    @Nested
    @DisplayName("Refresh Requirements")
    class RefreshRequirements {
        
        @Test
        void shouldNeedRefreshWhenNeverScraped() {
            // Given
            var location = createDefaultLocation();
            
            // Then
            assertThat(location.needsRefresh())
                .as("Location with no scraping data should need refresh")
                .isTrue();
        }
        
        @Test
        void shouldNeedRefreshWhenDataIsStale() {
            // Given
            var location = createDefaultLocation();
            LocalDateTime staleTimestamp = LocalDateTime.now().minusHours(25);
            location.updateScrapingData(50, staleTimestamp);
            
            // When
            boolean needsRefresh = location.needsRefresh();
            
            // Then
            assertThat(needsRefresh)
                .as("Location scraped >24h ago should need refresh")
                .isTrue();
        }
        
        @Test
        void shouldNotNeedRefreshWhenDataIsFresh() {
            // Given
            var location = createDefaultLocation();
            LocalDateTime freshTimestamp = LocalDateTime.now().minusHours(12);
            location.updateScrapingData(50, freshTimestamp);
            
            // When
            boolean needsRefresh = location.needsRefresh();
            
            // Then
            assertThat(needsRefresh)
                .as("Location scraped <24h ago should not need refresh")
                .isFalse();
        }
        
        @Test
        void shouldNeedRefreshAtExactly24HourBoundary() {
            // Given
            var location = createDefaultLocation();
            LocalDateTime exactlyOneDayAgo = LocalDateTime.now().minusHours(24).minusSeconds(1);
            location.updateScrapingData(50, exactlyOneDayAgo);
            
            // When
            boolean needsRefresh = location.needsRefresh();
            
            // Then
            assertThat(needsRefresh)
                .as("Location scraped exactly 24h ago should need refresh")
                .isTrue();
        }
    }
    
    @Nested
    @DisplayName("Bounding Box Update")
    class BoundingBoxUpdate {
        
        @Test
        void shouldUpdateBoundingBox() {
            // Given
            var location = createDefaultLocation();
            var newBbox = new BoundingBox(9.0, 45.0, 10.0, 46.0);
            
            // When
            location.setBoundingBox(newBbox);
            
            // Then
            assertThat(location.getBoundingBox()).isEqualTo(newBbox);
        }
    }
    
    private Location createDefaultLocation() {
        var coordinates = new Coordinates(MILAN_LAT, MILAN_LNG);
        var address = new Address("Milan", "Lombardy", "Italy", null);
        return new Location(coordinates, address);
    }
}
