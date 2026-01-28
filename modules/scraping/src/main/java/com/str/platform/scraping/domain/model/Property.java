package com.str.platform.scraping.domain.model;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Property entity.
 * Represents a short-term rental property from a platform.
 */
@Getter
public class Property extends BaseEntity {
    
    private UUID locationId;
    private ScrapingJob.Platform platform;
    private String platformId;
    private Coordinates coordinates;
    private String title;
    private PropertyType propertyType;
    private int bedrooms;
    private double bathrooms;
    private int maxGuests;
    private double rating;
    private int reviewCount;
    private Boolean isSuperhost;
    private String propertyUrl;
    private String imageUrl;
    
    /**
     * Full-year availability calendar data.
     * Each entry represents one month of availability.
     */
    private List<PropertyAvailability> availabilityCalendar = new ArrayList<>();
    
    /**
     * Collection of price samples across different date ranges.
     * Used for accurate ADR and seasonality calculation.
     */
    private List<PriceSample> priceSamples = new ArrayList<>();
    
    /**
     * Timestamp of last PDP (Property Detail Page) scrape
     */
    private Instant pdpLastScraped;
    
    /**
     * Timestamp of last availability calendar scrape
     */
    private Instant availabilityLastScraped;
    
    public enum PropertyType {
        ENTIRE_APARTMENT,
        ENTIRE_HOUSE,
        PRIVATE_ROOM,
        SHARED_ROOM
    }
    
    protected Property() {
        super();
    }
    
    public Property(
            UUID locationId,
            ScrapingJob.Platform platform,
            String platformId,
            Coordinates coordinates,
            PropertyType propertyType
    ) {
        super();
        this.locationId = locationId;
        this.platform = platform;
        this.platformId = platformId;
        this.coordinates = coordinates;
        this.propertyType = propertyType;
    }
    
    public void setDetails(int bedrooms, double bathrooms, int maxGuests) {
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.maxGuests = maxGuests;
        markAsUpdated();
    }
    
    public void setRating(double rating, int reviewCount) {
        this.rating = rating;
        this.reviewCount = reviewCount;
        markAsUpdated();
    }
    
    /**
     * Sets property metadata (title, URLs, host info).
     */
    public void setMetadata(String title, String propertyUrl, String imageUrl, Boolean isSuperhost) {
        this.title = title;
        this.propertyUrl = propertyUrl;
        this.imageUrl = imageUrl;
        this.isSuperhost = isSuperhost;
        markAsUpdated();
    }
    
    /**
     * Updates the availability calendar with fresh data.
     * Replaces existing calendar data.
     */
    public void updateAvailabilityCalendar(List<PropertyAvailability> availability) {
        this.availabilityCalendar = new ArrayList<>(availability);
        this.availabilityLastScraped = Instant.now();
        markAsUpdated();
    }
    
    /**
     * Adds a new price sample to the collection.
     */
    public void addPriceSample(PriceSample sample) {
        this.priceSamples.add(sample);
        markAsUpdated();
    }
    
    /**
     * Updates the PDP last scraped timestamp.
     */
    public void markPdpScraped() {
        this.pdpLastScraped = Instant.now();
        markAsUpdated();
    }
}
