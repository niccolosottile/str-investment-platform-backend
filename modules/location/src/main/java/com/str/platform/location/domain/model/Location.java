package com.str.platform.location.domain.model;

import com.str.platform.shared.domain.common.BaseEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Location aggregate root.
 * Represents a geographical location where investment opportunities exist.
 */
@Getter
public class Location extends BaseEntity {
    
    private Coordinates coordinates;
    private Address address;
    private BoundingBox boundingBox;
    private DataQuality dataQuality;
    private LocalDateTime lastScraped;
    private Integer propertyCount;
    private BigDecimal averagePrice;
    
    public enum DataQuality {
        HIGH,    // >50 properties, scraped <24h ago
        MEDIUM,  // 10-50 properties, scraped <7d ago
        LOW      // <10 properties or scraped >7d ago
    }
    
    protected Location() {
        super();
    }
    
    public Location(Coordinates coordinates, Address address) {
        super();
        this.coordinates = coordinates;
        this.address = address;
        this.dataQuality = DataQuality.LOW;
        this.propertyCount = 0;
    }
    
    public Location(Coordinates coordinates, Address address, BoundingBox boundingBox) {
        super();
        this.coordinates = coordinates;
        this.address = address;
        this.boundingBox = boundingBox;
        this.dataQuality = DataQuality.LOW;
        this.propertyCount = 0;
    }
    
    public void updateScrapingData(int propertyCount, LocalDateTime scrapedAt) {
        updateScrapingData(propertyCount, scrapedAt, null);
    }

    public void updateScrapingData(int propertyCount, LocalDateTime scrapedAt, BigDecimal averagePrice) {
        this.propertyCount = propertyCount;
        this.lastScraped = scrapedAt;
        this.averagePrice = averagePrice;
        this.dataQuality = calculateDataQuality(propertyCount, scrapedAt);
        markAsUpdated();
    }

    public void restoreScrapingData(Integer propertyCount, LocalDateTime scrapedAt, BigDecimal averagePrice) {
        this.propertyCount = propertyCount;
        this.lastScraped = scrapedAt;
        this.averagePrice = averagePrice;
        this.dataQuality = calculateDataQuality(propertyCount != null ? propertyCount : 0, scrapedAt);
    }
    
    private DataQuality calculateDataQuality(int count, LocalDateTime scrapedAt) {
        if (scrapedAt == null) {
            return DataQuality.LOW;
        }
        
        boolean recentData = scrapedAt.isAfter(LocalDateTime.now().minusDays(1));
        boolean moderateData = scrapedAt.isAfter(LocalDateTime.now().minusDays(7));
        
        if (count >= 50 && recentData) {
            return DataQuality.HIGH;
        } else if (count >= 10 && moderateData) {
            return DataQuality.MEDIUM;
        } else {
            return DataQuality.LOW;
        }
    }
    
    public boolean needsRefresh() {
        if (lastScraped == null) {
            return true;
        }
        return lastScraped.isBefore(LocalDateTime.now().minusHours(24));
    }
    
    public String getName() {
        return address.getFullAddress();
    }
    
    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
        markAsUpdated();
    }
}
