package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.Money;
import com.str.platform.scraping.infrastructure.persistence.entity.PriceSampleEntity;
import com.str.platform.scraping.infrastructure.persistence.entity.PropertyAvailabilityEntity;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyAvailabilityRepository;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPriceSampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing property data from scraping results.
 * Provides ADR, occupancy, and seasonality calculations based on actual scraped data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyDataAnalysisService {
    
    private final JpaPriceSampleRepository priceSampleRepository;
    private final JpaPropertyAvailabilityRepository availabilityRepository;
    
    /**
     * Calculate Average Daily Rate (ADR) from price samples.
     * Uses median to be robust against outliers.
     * 
     * @param locationId Location to analyze
     * @return ADR or null if insufficient data
     */
    public Money calculateAverageDailyRate(UUID locationId) {
        log.debug("Calculating ADR for location: {}", locationId);
        
        List<PriceSampleEntity> samples = priceSampleRepository.findByLocationId(locationId);
        
        if (samples.isEmpty()) {
            log.warn("No price samples found for location {}", locationId);
            return null;
        }
        
        // Calculate daily rate for each sample
        List<BigDecimal> dailyRates = samples.stream()
            .filter(sample -> sample.getNumberOfNights() > 0)
            .map(sample -> sample.getPrice()
                .divide(BigDecimal.valueOf(sample.getNumberOfNights()), 2, RoundingMode.HALF_UP))
            .sorted()
            .toList();
        
        if (dailyRates.isEmpty()) {
            log.warn("No valid daily rates calculated for location {}", locationId);
            return null;
        }
        
        // Return median (robust to outliers)
        BigDecimal median = calculateMedian(dailyRates);
        
        log.info("Calculated ADR for location {}: {} (from {} samples)",
            locationId, median, samples.size());
        
        return new Money(median, Money.Currency.EUR);
    }
    
    /**
     * Calculate seasonality index from price samples.
     * Formula: (max_monthly_avg - min_monthly_avg) / min_monthly_avg
     * 
     * @param locationId Location to analyze
     * @return Seasonality index (0 = no seasonality, >0 = seasonal variation)
     */
    public double calculateSeasonalityIndex(UUID locationId) {
        log.debug("Calculating seasonality index for location: {}", locationId);
        
        List<PriceSampleEntity> samples = priceSampleRepository.findByLocationId(locationId);
        
        if (samples.size() < 12) { // Need at least 12 samples across different months
            log.warn("Insufficient data for seasonality calculation: {} samples", samples.size());
            return 0.0; // No seasonality data
        }
        
        // Group samples by month and calculate average price per month
        Map<Integer, List<BigDecimal>> pricesByMonth = new HashMap<>();
        
        for (PriceSampleEntity sample : samples) {
            int month = sample.getSearchDateStart().getMonthValue();
            BigDecimal dailyRate = sample.getPrice()
                .divide(BigDecimal.valueOf(sample.getNumberOfNights()), 2, RoundingMode.HALF_UP);
            
            pricesByMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(dailyRate);
        }
        
        if (pricesByMonth.size() < 3) { // Need at least 3 different months
            log.warn("Data spans too few months for seasonality: {} months", pricesByMonth.size());
            return 0.0;
        }
        
        // Calculate average price for each month
        Map<Integer, BigDecimal> avgPricesByMonth = pricesByMonth.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    List<BigDecimal> prices = e.getValue();
                    BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    return sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
                }
            ));
        
        // Find max and min monthly averages
        BigDecimal maxMonthlyAvg = avgPricesByMonth.values().stream()
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        BigDecimal minMonthlyAvg = avgPricesByMonth.values().stream()
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        if (minMonthlyAvg.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        
        // Calculate seasonality index
        BigDecimal difference = maxMonthlyAvg.subtract(minMonthlyAvg);
        BigDecimal seasonalityIndex = difference.divide(minMonthlyAvg, 4, RoundingMode.HALF_UP);
        
        double result = seasonalityIndex.doubleValue();
        
        log.info("Calculated seasonality index for location {}: {} (max: {}, min: {})",
            locationId, String.format("%.2f", result), maxMonthlyAvg, minMonthlyAvg);
        
        return result;
    }
    
    /**
     * Calculate occupancy rate from ALL available monthly calendar data.
     * Uses all historical occupancy data that has been collected over time.
     * As the system runs longer, this provides increasingly accurate occupancy estimates.
     * 
     * @param locationId Location to analyze
     * @return Occupancy rate (0.0 to 1.0) or null if no data available
     */
    public BigDecimal calculateOccupancy(UUID locationId) {
        log.debug("Calculating occupancy for location: {}", locationId);
        
        // Get ALL availability data for the location (all months, all properties)
        List<PropertyAvailabilityEntity> availabilityData = 
            availabilityRepository.findLatestByLocationId(locationId);
        
        if (availabilityData.isEmpty()) {
            log.warn("No availability data found for location {}", locationId);
            return null;
        }
        
        // Calculate average occupancy across ALL months and properties
        BigDecimal totalOccupancy = availabilityData.stream()
            .map(PropertyAvailabilityEntity::getEstimatedOccupancy)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgOccupancy = totalOccupancy.divide(
            BigDecimal.valueOf(availabilityData.size()),
            4,
            RoundingMode.HALF_UP
        );
        
        log.info("Calculated occupancy for location {}: {} (from {} data points across {} months)",
            locationId, String.format("%.2f", avgOccupancy), 
            availabilityData.size(), 
            availabilityData.stream().map(PropertyAvailabilityEntity::getMonth).distinct().count());
        
        return avgOccupancy;
    }
    
    /**
     * Calculate median from sorted list of values
     */
    private BigDecimal calculateMedian(List<BigDecimal> sortedValues) {
        int size = sortedValues.size();
        
        if (size == 0) {
            return BigDecimal.ZERO;
        }
        
        if (size % 2 == 0) {
            // Even number: average of two middle values
            BigDecimal mid1 = sortedValues.get(size / 2 - 1);
            BigDecimal mid2 = sortedValues.get(size / 2);
            return mid1.add(mid2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            // Odd number: middle value
            return sortedValues.get(size / 2);
        }
    }
}
