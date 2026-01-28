package com.str.platform.analysis.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for storing investment analysis results.
 * Maps to the 'analysis_results' table created by Flyway migration V4.
 */
@Entity
@Table(name = "analysis_results", indexes = {
    @Index(name = "idx_analysis_location", columnList = "location_id"),
    @Index(name = "idx_analysis_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "investment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvestmentType investmentType;

    @Column(nullable = false)
    private BigDecimal budget;

    @Column(nullable = false)
    private String currency;

    @Column(name = "property_type")
    private String propertyType;

    // Investment Metrics (stored as JSON for flexibility)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private MetricsData metrics;

    // Market Analysis (stored as JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "market_analysis", columnDefinition = "jsonb")
    private MarketAnalysisData marketAnalysis;

    @Column(name = "data_quality", nullable = false)
    @Enumerated(EnumType.STRING)
    private DataQuality dataQuality;

    @Column(nullable = false)
    private Boolean cached;

    @Column(name = "cache_expires_at")
    private Instant cacheExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Investment type (buy vs rent)
     */
    public enum InvestmentType {
        BUY,
        RENT
    }

    /**
     * Data quality for analysis
     */
    public enum DataQuality {
        HIGH,
        MEDIUM,
        LOW
    }

    /**
     * Embedded metrics data structure for JSON storage
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsData {
        private BigDecimal monthlyRevenueConservative;
        private BigDecimal monthlyRevenueExpected;
        private BigDecimal monthlyRevenueOptimistic;
        private BigDecimal annualRoiConservative;
        private BigDecimal annualRoiExpected;
        private BigDecimal annualRoiOptimistic;
        private Integer paybackPeriodMonths;
        private BigDecimal occupancyRate;
        private BigDecimal averageDailyRate;
    }

    /**
     * Embedded market analysis data structure for JSON storage
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketAnalysisData {
        private Integer totalListings;
        private BigDecimal averageDailyRate;
        private BigDecimal occupancyRate;            // Average occupancy rate (0.0-1.0)
        private BigDecimal estimatedMonthlyRevenue;  // Estimated monthly revenue
        private String seasonality;
        private String growthTrend;
        private String competitionDensity;
        private Map<String, Object> additionalMetrics;
    }
}
