package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.MarketAnalysis;
import com.str.platform.analysis.domain.model.Money;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.scraping.domain.model.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for analyzing market conditions.
 * Provides insights into competition, pricing trends, and market dynamics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAnalysisService {
    
    private static final double SEARCH_RADIUS_KM = 5.0; // 5km radius for market analysis
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    // Competition density thresholds (properties per km²)
    private static final double LOW_DENSITY_THRESHOLD = 10.0;
    private static final double HIGH_DENSITY_THRESHOLD = 30.0;
    
    /**
     * Analyze market conditions based on available properties
     */
    public MarketAnalysis analyzeMarket(
            Coordinates location,
            List<Property> nearbyProperties
    ) {
        log.info("Analyzing market for location: {} with {} properties",
            location, nearbyProperties.size());
        
        if (nearbyProperties.isEmpty()) {
            log.warn("No properties found for market analysis at {}", location);
            return createEmptyAnalysis();
        }
        
        // Calculate average daily rate
        Money averageDailyRate = calculateAverageDailyRate(nearbyProperties);
        
        // Determine competition density
        MarketAnalysis.CompetitionDensity competitionDensity = 
            calculateCompetitionDensity(nearbyProperties.size());
        
        // Analyze growth trends based on pricing patterns
        MarketAnalysis.GrowthTrend growthTrend = 
            analyzeGrowthTrend(nearbyProperties);
        
        // Calculate seasonality index (simplified - would need historical data)
        double seasonalityIndex = calculateSeasonalityIndex();
        
        log.info("Market analysis complete: {} listings, €{} avg rate, {} competition",
            nearbyProperties.size(), averageDailyRate.getAmount(), competitionDensity);
        
        return new MarketAnalysis(
            nearbyProperties.size(),
            averageDailyRate,
            seasonalityIndex,
            growthTrend,
            competitionDensity
        );
    }
    
    /**
     * Calculate average daily rate from properties
     */
    private Money calculateAverageDailyRate(List<Property> properties) {
        // Filter out extreme outliers
        List<BigDecimal> prices = properties.stream()
            .map(Property::getPricePerNight)
            .sorted()
            .toList();
        
        // Remove top and bottom 10% as outliers
        int removeCount = (int) (prices.size() * 0.1);
        List<BigDecimal> filteredPrices = prices.subList(
            removeCount,
            prices.size() - removeCount
        );
        
        if (filteredPrices.isEmpty()) {
            filteredPrices = prices; // Fallback if too few properties
        }
        
        BigDecimal sum = filteredPrices.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal average = sum.divide(
            BigDecimal.valueOf(filteredPrices.size()),
            2,
            RoundingMode.HALF_UP
        );
        
        return new Money(average, Money.Currency.EUR);
    }
    
    /**
     * Calculate competition density based on number of properties
     */
    private MarketAnalysis.CompetitionDensity calculateCompetitionDensity(
            int propertyCount
    ) {
        // Calculate area of search circle (π × r²)
        double searchAreaKm2 = Math.PI * Math.pow(SEARCH_RADIUS_KM, 2);
        
        // Properties per km²
        double density = propertyCount / searchAreaKm2;
        
        log.debug("Competition density: {} properties/km² ({} properties in {}km² area)",
            String.format("%.2f", density), propertyCount, String.format("%.2f", searchAreaKm2));
        
        if (density < LOW_DENSITY_THRESHOLD) {
            return MarketAnalysis.CompetitionDensity.LOW;
        } else if (density < HIGH_DENSITY_THRESHOLD) {
            return MarketAnalysis.CompetitionDensity.MEDIUM;
        } else {
            return MarketAnalysis.CompetitionDensity.HIGH;
        }
    }
    
    /**
     * Analyze growth trends based on pricing distribution
     * 
     * Note: This is a simplified analysis. In production, this would:
     * - Use historical pricing data
     * - Compare to same period last year
     * - Track booking rate changes
     * - Monitor new listing velocity
     */
    private MarketAnalysis.GrowthTrend analyzeGrowthTrend(List<Property> properties) {
        // Group properties by rating/review count as proxy for maturity
        // Newer properties (low reviews) vs established (high reviews)
        
        double avgReviewsNew = properties.stream()
            .filter(p -> p.getReviewCount() < 10) // "New" properties
            .mapToDouble(Property::getReviewCount)
            .average()
            .orElse(0.0);
        
        double avgReviewsEstablished = properties.stream()
            .filter(p -> p.getReviewCount() >= 50) // "Established" properties
            .mapToDouble(Property::getReviewCount)
            .average()
            .orElse(0.0);
        
        // If many new properties with good reviews, market is growing
        // If few new properties, market may be declining or saturated
        long newPropertyCount = properties.stream()
            .filter(p -> p.getReviewCount() < 10)
            .count();
        
        double newPropertyRatio = (double) newPropertyCount / properties.size() * 100;
        
        log.debug("Growth analysis: {}% new properties, {} avg reviews (new), {} avg reviews (established)",
            String.format("%.1f", newPropertyRatio), 
            String.format("%.1f", avgReviewsNew),
            String.format("%.1f", avgReviewsEstablished));
        
        // Simplified heuristic:
        // - >25% new properties = Growing market
        // - <10% new properties = Declining/saturated market
        // - Otherwise = Stable
        
        if (newPropertyRatio > 25.0) {
            return MarketAnalysis.GrowthTrend.INCREASING;
        } else if (newPropertyRatio < 10.0 && properties.size() > 20) {
            return MarketAnalysis.GrowthTrend.DECLINING;
        } else {
            return MarketAnalysis.GrowthTrend.STABLE;
        }
    }
    
    /**
     * Calculate seasonality index
     * 
     * Note: This is a placeholder. Real implementation would:
     * - Use historical booking data
     * - Compare current month to annual average
     * - Account for local events and holidays
     * - Track seasonal pricing variations
     */
    private double calculateSeasonalityIndex() {
        // Return 1.0 (neutral) as default
        // >1.0 = High season, <1.0 = Low season
        return 1.0;
    }
    
    /**
     * Create empty analysis when no properties available
     */
    private MarketAnalysis createEmptyAnalysis() {
        return new MarketAnalysis(
            0,
            Money.euros(0),
            1.0,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.LOW
        );
    }
    
    /**
     * Calculate distance between two coordinates (Haversine formula)
     */
    private double calculateDistance(
            Coordinates from,
            Coordinates to
    ) {
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double deltaLat = Math.toRadians(to.getLatitude() - from.getLatitude());
        double deltaLng = Math.toRadians(to.getLongitude() - from.getLongitude());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Filter properties within radius
     */
    public List<Property> filterPropertiesWithinRadius(
            Coordinates center,
            List<Property> properties,
            double radiusKm
    ) {
        return properties.stream()
            .filter(property -> 
                calculateDistance(center, property.getCoordinates()) <= radiusKm
            )
            .toList();
    }
}
