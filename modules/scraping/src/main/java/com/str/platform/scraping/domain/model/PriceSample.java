package com.str.platform.scraping.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a single price sample for a property.
 * Multiple samples across different date ranges enable accurate ADR and seasonality calculation.
 */
@Getter
@AllArgsConstructor
public class PriceSample {
    
    /**
     * The total price for the stay
     */
    private final BigDecimal price;
    
    /**
     * Currency code (e.g., "EUR", "USD")
     */
    private final String currency;
    
    /**
     * Check-in date for this price sample
     */
    private final LocalDate searchDateStart;
    
    /**
     * Check-out date for this price sample
     */
    private final LocalDate searchDateEnd;
    
    /**
     * Number of nights in this stay
     */
    private final int numberOfNights;
    
    /**
     * Timestamp when this price was sampled
     */
    private final Instant sampledAt;
    
    /**
     * Calculates the average daily rate from this price sample.
     * 
     * @return ADR (price per night)
     */
    public BigDecimal getAverageDailyRate() {
        if (numberOfNights <= 0) {
            return BigDecimal.ZERO;
        }
        return price.divide(BigDecimal.valueOf(numberOfNights), 2, java.math.RoundingMode.HALF_UP);
    }
}
