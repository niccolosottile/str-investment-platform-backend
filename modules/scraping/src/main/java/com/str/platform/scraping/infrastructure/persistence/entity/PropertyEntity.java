package com.str.platform.scraping.infrastructure.persistence.entity;

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
 * JPA entity for scraped property data.
 * Maps to the 'properties' table created by Flyway migration V2.
 */
@Entity
@Table(name = "properties", indexes = {
    @Index(name = "idx_property_location", columnList = "location_id"),
    @Index(name = "idx_property_platform", columnList = "platform, platform_property_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_platform_property", columnNames = {"platform", "platform_property_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "platform_property_id", nullable = false)
    private String platformPropertyId;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(nullable = false)
    private String title;

    @Column(name = "property_type")
    private String propertyType;

    private Integer bedrooms;

    private Integer bathrooms;

    private Integer guests;

    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "is_superhost")
    private Boolean isSuperhost;

    @Column(name = "property_url")
    private String propertyUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Supported STR platforms
     */
    public enum Platform {
        AIRBNB,
        BOOKING,
        VRBO
    }
}
