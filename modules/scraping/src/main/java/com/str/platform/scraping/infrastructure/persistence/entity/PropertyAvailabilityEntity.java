package com.str.platform.scraping.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for property availability calendar data.
 * Stores monthly availability snapshots for occupancy analysis.
 */
@Entity
@Table(name = "property_availability", indexes = {
    @Index(name = "idx_property_availability_property", columnList = "property_id"),
    @Index(name = "idx_property_availability_month", columnList = "property_id, month"),
    @Index(name = "idx_property_availability_scraped", columnList = "scraped_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_property_month_scraped", columnNames = {"property_id", "month", "scraped_at"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    /**
     * Month in YYYY-MM format (e.g., "2026-02")
     */
    @Column(nullable = false, length = 7)
    private String month;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "available_days", nullable = false)
    private Integer availableDays;

    @Column(name = "booked_days", nullable = false)
    private Integer bookedDays;

    @Column(name = "blocked_days", nullable = false)
    private Integer blockedDays;

    /**
     * Estimated occupancy rate: booked_days / (total_days - blocked_days)
     * Range: 0.0000 to 1.0000
     */
    @Column(name = "estimated_occupancy", nullable = false, precision = 5, scale = 4)
    private BigDecimal estimatedOccupancy;

    @Column(name = "scraped_at", nullable = false)
    private Instant scrapedAt;
}
