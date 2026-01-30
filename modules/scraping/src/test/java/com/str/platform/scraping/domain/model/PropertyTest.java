package com.str.platform.scraping.domain.model;

import com.str.platform.location.domain.model.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Property entity.
 * Tests property metadata management and price/availability data updates.
 */
class PropertyTest {
    
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final String PLATFORM_ID = "airbnb-12345";
    private static final Coordinates MILAN_COORDS = new Coordinates(45.4642, 9.1900);
    
    @Nested
    @DisplayName("Property Creation")
    class PropertyCreation {
        
        @Test
        void shouldCreatePropertyWithMinimalData() {
            // When
            var property = new Property(
                LOCATION_ID,
                ScrapingJob.Platform.AIRBNB,
                PLATFORM_ID,
                MILAN_COORDS,
                Property.PropertyType.ENTIRE_APARTMENT
            );
            
            // Then
            assertThat(property)
                .satisfies(p -> {
                    assertThat(p.getLocationId()).isEqualTo(LOCATION_ID);
                    assertThat(p.getPlatform()).isEqualTo(ScrapingJob.Platform.AIRBNB);
                    assertThat(p.getPlatformId()).isEqualTo(PLATFORM_ID);
                    assertThat(p.getCoordinates()).isEqualTo(MILAN_COORDS);
                    assertThat(p.getPropertyType()).isEqualTo(Property.PropertyType.ENTIRE_APARTMENT);
                    assertThat(p.getAvailabilityCalendar()).isEmpty();
                    assertThat(p.getPriceSamples()).isEmpty();
                });
        }
    }
    
    @Nested
    @DisplayName("Property Details Management")
    class PropertyDetailsManagement {
        
        private Property property;
        
        @BeforeEach
        void setUp() {
            property = createDefaultProperty();
        }
        
        @Test
        void shouldSetPropertyDetails() {
            // When
            property.setDetails(2, 1.5, 4);
            
            // Then
            assertThat(property.getBedrooms()).isEqualTo(2);
            assertThat(property.getBathrooms()).isEqualTo(1.5);
            assertThat(property.getMaxGuests()).isEqualTo(4);
        }
        
        @Test
        void shouldSetRatingAndReviews() {
            // When
            property.setRating(4.8, 127);
            
            // Then
            assertThat(property.getRating()).isEqualTo(4.8);
            assertThat(property.getReviewCount()).isEqualTo(127);
        }
        
        @Test
        void shouldSetMetadata() {
            // Given
            String title = "Beautiful Apartment in Milan";
            String url = "https://airbnb.com/rooms/12345";
            String imageUrl = "https://images.airbnb.com/property.jpg";
            
            // When
            property.setMetadata(title, url, imageUrl, true);
            
            // Then
            assertThat(property.getTitle()).isEqualTo(title);
            assertThat(property.getPropertyUrl()).isEqualTo(url);
            assertThat(property.getImageUrl()).isEqualTo(imageUrl);
            assertThat(property.getIsSuperhost()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Availability Calendar Management")
    class AvailabilityCalendarManagement {
        
        private Property property;
        
        @BeforeEach
        void setUp() {
            property = createDefaultProperty();
        }
        
        @Test
        void shouldUpdateAvailabilityCalendar() {
            // Given
            List<PropertyAvailability> calendar = createSampleCalendar();
            
            // When
            property.updateAvailabilityCalendar(calendar);
            
            // Then
            assertThat(property.getAvailabilityCalendar())
                .hasSize(3)
                .isNotSameAs(calendar); // Should be a copy
            assertThat(property.getAvailabilityLastScraped())
                .as("Last scraped timestamp should be set")
                .isNotNull()
                .isBeforeOrEqualTo(Instant.now());
        }
        
        @Test
        void shouldReplaceExistingCalendarData() {
            // Given
            property.updateAvailabilityCalendar(createSampleCalendar());
            
            // When - Update with new data
            List<PropertyAvailability> newCalendar = List.of(
                createAvailability(YearMonth.of(2026, 7))
            );
            property.updateAvailabilityCalendar(newCalendar);
            
            // Then
            assertThat(property.getAvailabilityCalendar())
                .hasSize(1)
                .extracting(PropertyAvailability::getMonth)
                .containsExactly(YearMonth.of(2026, 7));
        }
        
        @Test
        void shouldHandleEmptyCalendar() {
            // When
            property.updateAvailabilityCalendar(new ArrayList<>());
            
            // Then
            assertThat(property.getAvailabilityCalendar()).isEmpty();
            assertThat(property.getAvailabilityLastScraped()).isNotNull();
        }
    }
    
    
    @Nested
    @DisplayName("Price Sample Management")
    class PriceSampleManagement {
        
        private Property property;
        
        @BeforeEach
        void setUp() {
            property = createDefaultProperty();
        }
        
        @Test
        void shouldAddPriceSample() {
            // Given
            var priceSample = createPriceSample(new BigDecimal("600.00"), 3);
            
            // When
            property.addPriceSample(priceSample);
            
            // Then
            assertThat(property.getPriceSamples())
                .hasSize(1)
                .contains(priceSample);
        }
        
        @Test
        void shouldAccumulatePriceSamples() {
            // Given
            var sample1 = createPriceSample(new BigDecimal("600.00"), 3);
            var sample2 = createPriceSample(new BigDecimal("800.00"), 4);
            var sample3 = createPriceSample(new BigDecimal("450.00"), 2);
            
            // When
            property.addPriceSample(sample1);
            property.addPriceSample(sample2);
            property.addPriceSample(sample3);
            
            // Then
            assertThat(property.getPriceSamples())
                .as("Price samples should accumulate, not replace")
                .hasSize(3)
                .containsExactly(sample1, sample2, sample3);
        }
    }
    
    @Nested
    @DisplayName("Scraping Timestamp Management")
    class ScrapingTimestampManagement {
        
        private Property property;
        
        @BeforeEach
        void setUp() {
            property = createDefaultProperty();
        }
        
        @Test
        void shouldMarkPdpAsScraped() {
            // When
            property.markPdpScraped();
            
            // Then
            assertThat(property.getPdpLastScraped())
                .as("PDP last scraped timestamp should be set")
                .isNotNull()
                .isBeforeOrEqualTo(Instant.now());
        }
        
        @Test
        void shouldUpdatePdpTimestampOnSubsequentCalls() {
            // Given
            property.markPdpScraped();
            Instant firstTimestamp = property.getPdpLastScraped();
            
            // When - Simulate passage of time
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            property.markPdpScraped();
            
            // Then
            assertThat(property.getPdpLastScraped())
                .as("Timestamp should be updated on subsequent scrapes")
                .isAfterOrEqualTo(firstTimestamp);
        }
    }
    
    
    private Property createDefaultProperty() {
        return new Property(
            LOCATION_ID,
            ScrapingJob.Platform.AIRBNB,
            PLATFORM_ID,
            MILAN_COORDS,
            Property.PropertyType.ENTIRE_APARTMENT
        );
    }
    
    private List<PropertyAvailability> createSampleCalendar() {
        return List.of(
            createAvailability(YearMonth.of(2026, 6)),
            createAvailability(YearMonth.of(2026, 7)),
            createAvailability(YearMonth.of(2026, 8))
        );
    }
    
    private PropertyAvailability createAvailability(YearMonth month) {
        return new PropertyAvailability(
            month,
            30,
            10,  // available
            18,  // booked
            2,   // blocked
            0.64 // occupancy
        );
    }
    
    private PriceSample createPriceSample(BigDecimal price, int nights) {
        return new PriceSample(
            price,
            "EUR",
            LocalDate.now(),
            LocalDate.now().plusDays(nights),
            nights,
            Instant.now()
        );
    }
}
