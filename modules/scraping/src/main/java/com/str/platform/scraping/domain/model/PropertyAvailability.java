package com.str.platform.scraping.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.YearMonth;

/**
 * Represents monthly availability data for a property.
 * Used for occupancy estimation.
 */
@Getter
@AllArgsConstructor
public class PropertyAvailability {
    
    /**
     * The month this availability data represents (e.g., "2026-02")
     */
    private final YearMonth month;
    
    /**
     * Total number of days in the month
     */
    private final int totalDays;
    
    /**
     * Number of days available for booking
     */
    private final int availableDays;
    
    /**
     * Number of days already booked by guests
     */
    private final int bookedDays;
    
    /**
     * Number of days blocked by the host (unavailable)
     */
    private final int blockedDays;
    
    /**
     * Estimated occupancy rate for this month.
     * Calculated as: bookedDays / (totalDays - blockedDays)
     */
    private final double estimatedOccupancy;
    
    /**
     * Calculates occupancy rate from availability data.
     * 
     * @param totalDays Total days in month
     * @param availableDays Days available for booking
     * @param bookedDays Days already booked
     * @param blockedDays Days blocked by host
     * @return Occupancy rate between 0.0 and 1.0
     */
    public static double calculateOccupancy(int totalDays, int availableDays, int bookedDays, int blockedDays) {
        int availableForBooking = totalDays - blockedDays;
        if (availableForBooking <= 0) {
            return 0.0;
        }
        return (double) bookedDays / availableForBooking;
    }
}
