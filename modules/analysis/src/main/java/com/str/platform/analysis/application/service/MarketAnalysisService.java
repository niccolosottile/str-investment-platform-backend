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
import java.util.UUID;

/**
 * Service for analyzing market conditions.
 * Provides insights into competition, pricing trends, and market dynamics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketAnalysisService {
    
    private final PropertyDataAnalysisService propertyDataAnalysisService;
    
    private static final double SEARCH_RADIUS_KM = 5.0; // 5km radius for market analysis
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    // Competition density thresholds (properties per km²)
    private static final double LOW_DENSITY_THRESHOLD = 10.0;
    private static final double HIGH_DENSITY_THRESHOLD = 30.0;
    
    /**
     * Analyze market conditions based on available properties and scraped data.
     * Uses PropertyDataAnalysisService to get ADR and seasonality from actual price samples.
     * 
     * @param locationId Location identifier for querying scraped data
     * @param location Coordinates for spatial analysis
     * @param nearbyProperties Properties for competition and growth analysis
     * @return Complete market analysis
     */
    public MarketAnalysis analyzeMarket(
            UUID locationId,
            Coordinates location,
            List<Property> nearbyProperties
    ) {
        log.info("Analyzing market for location: {} ({}) with {} properties",
            locationId, location, nearbyProperties.size());
        
        if (nearbyProperties.isEmpty()) {
            log.error("No properties found for market analysis at location {}", locationId);
            return null;
        }
        
        // Calculate average daily rate from scraped price samples
        Money averageDailyRate = propertyDataAnalysisService.calculateAverageDailyRate(locationId);
        if (averageDailyRate == null) {
            log.error("No price sample data available for location {}, cannot perform analysis", locationId);
            return null;
        }
        
        // Calculate occupancy rate from availability calendar data
        BigDecimal occupancyRate = propertyDataAnalysisService.calculateOccupancy(locationId);
        if (occupancyRate == null) {
            log.error("No availability data for location {}, cannot perform analysis", locationId);
            return null;
        }
        
        // Calculate estimated monthly revenue
        Money monthlyRevenue = propertyDataAnalysisService.calculateMonthlyRevenue(locationId);
        if (monthlyRevenue == null) {
            log.error("Cannot calculate monthly revenue for location {}", locationId);
            return null;
        }
        
        // Determine competition density
        MarketAnalysis.CompetitionDensity competitionDensity = 
            calculateCompetitionDensity(nearbyProperties.size());
        
        // Analyze growth trends based on pricing patterns
        MarketAnalysis.GrowthTrend growthTrend = 
            analyzeGrowthTrend(nearbyProperties);
        
        // Calculate seasonality index from scraped price samples
        double seasonalityIndex = propertyDataAnalysisService.calculateSeasonalityIndex(locationId);
        
        log.info("Market analysis complete: {} listings, €{} ADR, {}% occupancy, €{} monthly revenue, seasonality {}, {} competition",
            nearbyProperties.size(), averageDailyRate.getAmount(), 
            String.format("%.1f", occupancyRate.multiply(BigDecimal.valueOf(100))),
            monthlyRevenue.getAmount(),
            String.format("%.2f", seasonalityIndex), competitionDensity);
        
        return new MarketAnalysis(
            nearbyProperties.size(),
            averageDailyRate,
            occupancyRate,
            monthlyRevenue,
            seasonalityIndex,
            growthTrend,
            competitionDensity
        );
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
