package com.str.platform.scraping.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.str.platform.scraping.domain.model.JobType;
import com.str.platform.scraping.domain.model.PropertyAvailability;
import com.str.platform.scraping.domain.model.PriceSample;
import com.str.platform.shared.domain.common.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a scraping job completes successfully.
 * Includes the scraped property data from the Python worker.
 */
@Getter
@AllArgsConstructor
public class ScrapingJobCompletedEvent implements DomainEvent {
    
    private final UUID jobId;
    private final JobType jobType;
    private final UUID locationId;
    private final LocalDate searchDateStart;
    private final LocalDate searchDateEnd;
    private final int propertiesFound;
    private final List<PropertyData> properties;
    private final Integer duplicatesRemoved;
    private final Integer filteredOutOfBounds;
    private final LocalDateTime occurredAt;
    
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
    
    /**
     * Property data DTO for scraping results
     */
    @Getter
    public static class PropertyData {
        private final String platformId;
        private final String platform;
        private final double latitude;
        private final double longitude;
        private final String title;
        private final String propertyType;
        private final java.math.BigDecimal price;
        private final String currency;
        private final Integer bedrooms;
        private final Integer bathrooms;
        private final Integer guests;
        private final java.math.BigDecimal rating;
        private final Integer reviewCount;
        private final Boolean isSuperhost;
        private final String imageUrl;
        private final String propertyUrl;
        private final List<String> amenities;
        private final List<PropertyAvailability> availability;
        private final PriceSample priceSample;
        private final String dataCompleteness;
        private final LocalDateTime pdpLastScraped;
        
        @JsonCreator
        public PropertyData(
                @JsonProperty("platformId") String platformId,
                @JsonProperty("platform") String platform,
                @JsonProperty("latitude") double latitude,
                @JsonProperty("longitude") double longitude,
                @JsonProperty("title") String title,
                @JsonProperty("propertyType") String propertyType,
                @JsonProperty("price") java.math.BigDecimal price,
                @JsonProperty("currency") String currency,
                @JsonProperty("bedrooms") Integer bedrooms,
                @JsonProperty("bathrooms") Integer bathrooms,
                @JsonProperty("guests") Integer guests,
                @JsonProperty("rating") java.math.BigDecimal rating,
                @JsonProperty("reviewCount") Integer reviewCount,
                @JsonProperty("isSuperhost") Boolean isSuperhost,
                @JsonProperty("imageUrl") String imageUrl,
                @JsonProperty("propertyUrl") String propertyUrl,
                @JsonProperty("amenities") List<String> amenities,
                @JsonProperty("availability") List<PropertyAvailability> availability,
                @JsonProperty("priceSample") PriceSample priceSample,
                @JsonProperty("dataCompleteness") String dataCompleteness,
                @JsonProperty("pdpLastScraped") LocalDateTime pdpLastScraped
        ) {
            this.platformId = platformId;
            this.platform = platform;
            this.latitude = latitude;
            this.longitude = longitude;
            this.title = title;
            this.propertyType = propertyType;
            this.price = price;
            this.currency = currency;
            this.bedrooms = bedrooms;
            this.bathrooms = bathrooms;
            this.guests = guests;
            this.rating = rating;
            this.reviewCount = reviewCount;
            this.isSuperhost = isSuperhost;
            this.imageUrl = imageUrl;
            this.propertyUrl = propertyUrl;
            this.amenities = amenities;
            this.availability = availability;
            this.priceSample = priceSample;
            this.dataCompleteness = dataCompleteness;
            this.pdpLastScraped = pdpLastScraped;
        }
    }
}
