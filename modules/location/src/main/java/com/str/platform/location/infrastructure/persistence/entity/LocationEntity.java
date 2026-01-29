package com.str.platform.location.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for location data persistence.
 */
@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_location_coordinates", columnList = "latitude, longitude"),
    @Index(name = "idx_location_city_country", columnList = "city, country")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String city;

    private String region;

    @Column(nullable = false)
    private String country;

    @Column(name = "full_address")
    private String fullAddress;
    
    @Column(name = "bbox_sw_lng", precision = 11, scale = 8)
    private BigDecimal boundingBoxSwLng;
    
    @Column(name = "bbox_sw_lat", precision = 10, scale = 8)
    private BigDecimal boundingBoxSwLat;
    
    @Column(name = "bbox_ne_lng", precision = 11, scale = 8)
    private BigDecimal boundingBoxNeLng;
    
    @Column(name = "bbox_ne_lat", precision = 10, scale = 8)
    private BigDecimal boundingBoxNeLat;

    @Column(name = "data_quality", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataQuality dataQuality;

    @Column(name = "last_scraped")
    private Instant lastScraped;

    @Column(name = "property_count")
    private Integer propertyCount;

    @Column(name = "average_price")
    private BigDecimal averagePrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Data quality levels for location information
     */
    public enum DataQuality {
        HIGH,    // Recently scraped (<7 days), many properties
        MEDIUM,  // Scraped within 30 days, some properties
        LOW      // Stale data (>30 days) or few properties
    }
}
