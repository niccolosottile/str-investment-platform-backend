package com.str.platform.scraping.domain.model;

import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

import java.math.BigDecimal;
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
    private BigDecimal pricePerNight;
    private String currency;
    private PropertyType propertyType;
    private int bedrooms;
    private double bathrooms;
    private int maxGuests;
    private double rating;
    private int reviewCount;
    
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
            BigDecimal pricePerNight,
            PropertyType propertyType
    ) {
        super();
        this.locationId = locationId;
        this.platform = platform;
        this.platformId = platformId;
        this.coordinates = coordinates;
        this.pricePerNight = pricePerNight;
        this.currency = "EUR";
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
}
