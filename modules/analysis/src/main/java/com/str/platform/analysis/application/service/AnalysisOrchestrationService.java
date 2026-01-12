package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.mapper.AnalysisResultEntityMapper;
import com.str.platform.analysis.infrastructure.persistence.repository.JpaAnalysisResultRepository;
import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.Location;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.infrastructure.persistence.repository.JpaPropertyRepository;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Main orchestration service for investment analysis.
 * Coordinates market analysis, investment calculations, and result persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrationService {
    
    private final InvestmentAnalysisService investmentAnalysisService;
    private final MarketAnalysisService marketAnalysisService;
    private final JpaPropertyRepository propertyRepository;
    private final JpaAnalysisResultRepository analysisResultRepository;
    private final AnalysisResultEntityMapper analysisResultMapper;
    private final LocationService locationService;
    
    private static final double SEARCH_RADIUS_KM = 5.0;
    
    /**
     * Perform complete investment analysis for an existing Location.
     */
    @Transactional
    @Cacheable(value = "analysisResults", key = "#locationId.toString() + '-' + #budget.amount")
    public AnalysisResult performAnalysisForLocation(
            UUID locationId,
            InvestmentConfiguration.InvestmentType investmentType,
            Money budget,
            InvestmentConfiguration.PropertyType propertyType,
            InvestmentConfiguration.InvestmentGoal goal,
            boolean acceptsRenovation
    ) {
        Location location = locationService.getById(locationId);

        InvestmentConfiguration config = new InvestmentConfiguration(
            location.getCoordinates(),
            investmentType,
            budget,
            propertyType,
            goal
        );

        if (acceptsRenovation) {
            config.setAcceptsRenovation(true);
        }

        log.info("Starting investment analysis for locationId={} {} with budget: {}",
            locationId, config.getLocation(), config.getBudget());
        
        // 1. Fetch nearby properties (within 5km radius)
        List<Property> nearbyProperties = fetchNearbyProperties(config);
        
        if (nearbyProperties.isEmpty()) {
            log.warn("No properties found near {}. Returning empty analysis.", 
                config.getLocation());
            return createEmptyAnalysis(config);
        }
        
        // 2. Perform market analysis
        MarketAnalysis marketAnalysis = marketAnalysisService.analyzeMarket(
            config.getLocation(),
            nearbyProperties
        );
        
        // 3. Calculate investment metrics
        InvestmentMetrics metrics = investmentAnalysisService.calculateMetrics(
            config,
            marketAnalysis.getAverageDailyRate(),
            nearbyProperties.size()
        );
        
        // 4. Determine data quality
        AnalysisResult.DataQuality dataQuality = 
            investmentAnalysisService.determineDataQuality(nearbyProperties.size());
        
        // 5. Create and save analysis result
        AnalysisResult result = new AnalysisResult(
            config,
            metrics,
            marketAnalysis,
            dataQuality
        );

        // Persist to database
        var entity = analysisResultMapper.toEntity(result, locationId);
        var savedEntity = analysisResultRepository.save(entity);
        
        log.info("Analysis complete: ROI={}%, Payback={} months, DataQuality={}",
            String.format("%.2f", metrics.getAnnualROI()),
            metrics.getPaybackPeriodMonths(),
            dataQuality);
        
        return analysisResultMapper.toDomain(savedEntity);
    }
    
    /**
     * Fetch an existing analysis by ID
     */
    @Transactional(readOnly = true)
    public AnalysisResult getAnalysis(UUID analysisId) {
        log.info("Fetching analysis: {}", analysisId);
        
        return analysisResultRepository.findById(analysisId)
            .map(analysisResultMapper::toDomain)
            .orElseThrow(() -> new EntityNotFoundException("AnalysisResult", analysisId.toString()));
    }
    
    /**
     * Check if analysis results need refresh (older than 6 hours)
     */
    @Transactional(readOnly = true)
    public boolean needsRefresh(UUID analysisId) {
        return analysisResultRepository.findById(analysisId)
            .map(entity -> {
                AnalysisResult result = analysisResultMapper.toDomain(entity);
                return result.needsRefresh();
            })
            .orElse(true);
    }
    
    /**
     * Fetch properties near the investment location
     */
    private List<Property> fetchNearbyProperties(InvestmentConfiguration config) {
        // Calculate bounding box for search (approximate)
        double latDelta = SEARCH_RADIUS_KM / 111.0; // 1 degree lat ≈ 111 km
        double lngDelta = SEARCH_RADIUS_KM / (111.0 * Math.cos(Math.toRadians(config.getLocation().getLatitude())));
        
        BigDecimal minLat = BigDecimal.valueOf(config.getLocation().getLatitude() - latDelta);
        BigDecimal maxLat = BigDecimal.valueOf(config.getLocation().getLatitude() + latDelta);
        BigDecimal minLng = BigDecimal.valueOf(config.getLocation().getLongitude() - lngDelta);
        BigDecimal maxLng = BigDecimal.valueOf(config.getLocation().getLongitude() + lngDelta);
        
        log.debug("Searching properties in bounds: lat[{}, {}], lng[{}, {}]",
            minLat, maxLat, minLng, maxLng);
        
        // Fetch from database
        var entities = propertyRepository.findWithinBounds(minLat, maxLat, minLng, maxLng);
        
        log.info("Found {} properties in bounding box", entities.size());
        
        // Convert entities to domain objects
        // Note: We need to create a PropertyEntityMapper for this
        // For now, create domain objects manually
        return entities.stream()
            .map(entity -> {
                var coords = new com.str.platform.location.domain.model.Coordinates(
                    entity.getLatitude().doubleValue(),
                    entity.getLongitude().doubleValue()
                );
                
                var property = new Property(
                    entity.getLocationId(),
                    mapPlatform(entity.getPlatform()),
                    entity.getPlatformPropertyId(),
                    coords,
                    entity.getPrice(), // PropertyEntity uses 'price' not 'pricePerNight'
                    mapPropertyType(entity.getPropertyType())
                );
                
                // Set additional details
                property.setDetails(
                    entity.getBedrooms() != null ? entity.getBedrooms() : 0,
                    entity.getBathrooms() != null ? entity.getBathrooms().doubleValue() : 0.0,
                    entity.getGuests() != null ? entity.getGuests() : 0
                );
                
                // Set rating
                if (entity.getRating() != null) {
                    property.setRating(
                        entity.getRating().doubleValue(),
                        entity.getReviewCount() != null ? entity.getReviewCount() : 0
                    );
                }
                
                return property;
            })
            .toList();
    }
    
    /**
     * Create empty analysis when no data available
     */
    private AnalysisResult createEmptyAnalysis(InvestmentConfiguration config) {
        MarketAnalysis emptyMarket = new MarketAnalysis(
            0,
            Money.euros(0),
            1.0,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.LOW
        );
        
        InvestmentMetrics emptyMetrics = new InvestmentMetrics(
            Money.euros(0),
            Money.euros(0),
            Money.euros(0),
            0.0,
            Integer.MAX_VALUE,
            0.0
        );
        
        return new AnalysisResult(
            config,
            emptyMetrics,
            emptyMarket,
            AnalysisResult.DataQuality.LOW
        );
    }
    
    /**
     * Map entity platform to domain platform
     */
    private com.str.platform.scraping.domain.model.ScrapingJob.Platform mapPlatform(
            com.str.platform.scraping.infrastructure.persistence.entity.PropertyEntity.Platform entityPlatform
    ) {
        return switch (entityPlatform) {
            case AIRBNB -> com.str.platform.scraping.domain.model.ScrapingJob.Platform.AIRBNB;
            case BOOKING -> com.str.platform.scraping.domain.model.ScrapingJob.Platform.BOOKING;
            case VRBO -> com.str.platform.scraping.domain.model.ScrapingJob.Platform.VRBO;
        };
    }
    
    /**
     * Map entity property type string to domain property type
     */
    private Property.PropertyType mapPropertyType(String entityType) {
        if (entityType == null) {
            return Property.PropertyType.ENTIRE_APARTMENT; // Default
        }
        
        return switch (entityType.toUpperCase()) {
            case "ENTIRE_APARTMENT", "ENTIRE_APT", "APARTMENT" -> Property.PropertyType.ENTIRE_APARTMENT;
            case "ENTIRE_HOUSE", "HOUSE" -> Property.PropertyType.ENTIRE_HOUSE;
            case "PRIVATE_ROOM", "ROOM" -> Property.PropertyType.PRIVATE_ROOM;
            case "SHARED_ROOM" -> Property.PropertyType.SHARED_ROOM;
            default -> Property.PropertyType.ENTIRE_APARTMENT; // Default fallback
        };
    }
}
