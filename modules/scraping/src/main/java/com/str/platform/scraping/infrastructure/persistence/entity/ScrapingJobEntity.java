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
 * JPA entity for scraping job tracking.
 * Maps to the 'scraping_jobs' table created by Flyway migration V3.
 */
@Entity
@Table(name = "scraping_jobs", indexes = {
    @Index(name = "idx_scraping_job_status", columnList = "status"),
    @Index(name = "idx_scraping_job_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapingJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "radius_km", nullable = false)
    private Integer radiusKm;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "properties_found")
    private Integer propertiesFound;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Scraping job execution status
     */
    public enum JobStatus {
        PENDING,       // Job created, not yet started
        IN_PROGRESS,   // Currently being processed by Python worker
        COMPLETED,     // Successfully completed
        FAILED         // Failed with error
    }

    /**
     * Supported STR platforms
     */
    public enum Platform {
        AIRBNB,
        BOOKING_COM,
        VRBO
    }
}
