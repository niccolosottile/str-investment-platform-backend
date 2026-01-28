package com.str.platform.scraping.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for property price sample data.
 * Stores price samples across multiple date ranges for ADR and seasonality calculation.
 */
@Entity
@Table(name = "price_samples", indexes = {
    @Index(name = "idx_price_samples_property", columnList = "property_id"),
    @Index(name = "idx_price_samples_dates", columnList = "property_id, search_date_start"),
    @Index(name = "idx_price_samples_sampled", columnList = "sampled_at"),
    @Index(name = "idx_price_samples_date_range", columnList = "search_date_start, search_date_end")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceSampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    /**
     * Total price for the entire stay period
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "search_date_start", nullable = false)
    private LocalDate searchDateStart;

    @Column(name = "search_date_end", nullable = false)
    private LocalDate searchDateEnd;

    /**
     * Duration of stay, calculated from date range
     */
    @Column(name = "number_of_nights", nullable = false)
    private Integer numberOfNights;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt;
}
