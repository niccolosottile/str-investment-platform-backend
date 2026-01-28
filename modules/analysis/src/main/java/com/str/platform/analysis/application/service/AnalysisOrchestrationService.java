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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Perform complete investment analysis for an existing Location.
     * Results are cached per location + configuration parameters.
     * Cache key includes all parameters that affect the analysis result.
     */
    @Transactional
    @Cacheable(value = "analysisResults", 
               key = "#locationId + '-' + #investmentType + '-' + #budget.amount + '-' + #propertyType + '-' + #goal + '-' + #acceptsRenovation")
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
        
        // 1. Fetch properties for this location
        List<Property> properties = fetchPropertiesByLocation(locationId);
        
        if (properties.isEmpty()) {
            log.warn("No properties found for location {}. Returning empty analysis.", locationId);
            return createEmptyAnalysis(config);
        }
        
        // 2. Perform market analysis
        MarketAnalysis marketAnalysis = marketAnalysisService.analyzeMarket(
            config.getLocation(),
            properties
        );
        
        // 3. Calculate investment metrics
        InvestmentMetrics metrics = investmentAnalysisService.calculateMetrics(
            config,
            marketAnalysis.getAverageDailyRate(),
            properties.size()
        );
        
        // 4. Determine data quality
        AnalysisResult.DataQuality dataQuality = 
            investmentAnalysisService.determineDataQuality(properties.size());
        
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
     * Fetch all properties for a specific location.
     */
    private List<Property> fetchPropertiesByLocation(UUID locationId) {
        log.debug("Fetching properties for location: {}", locationId);
        
        var entities = propertyRepository.findByLocationId(locationId);
        
        log.info("Found {} properties for location {}", entities.size(), locationId);
        
        // Convert entities to domain objects
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
                    entity.getPrice(),
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
    
    /**
     * Evict all cached analysis results for a specific location.
     * Called when new property data becomes available for a location.
     * 
     * Uses Redis pattern matching to find all cache entries starting with locationId,
     * regardless of other parameters (budget, type, goal, etc).
     * 
     * Cache keys follow pattern: "analysisResults::{locationId}-{params}"
     */
    public void evictAnalysisCacheForLocation(UUID locationId) {
        try {
            // Spring's RedisCacheManager uses "cacheName::key" format
            // Pattern matches all entries for this location regardless of parameters
            String pattern = "analysisResults::" + locationId.toString() + "-*";
            
            // Use pattern matching to find all keys for this location
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("Evicted {} analysis cache entries for locationId={}", deleted, locationId);
            } else {
                log.debug("No analysis cache entries found for locationId={}", locationId);
            }
        } catch (Exception e) {
            // Log but don't fail - cache eviction is non-critical
            // Stale cache entries will expire naturally via TTL
            log.error("Failed to evict analysis cache for locationId={}: {}", locationId, e.getMessage());
        }
    }
}
