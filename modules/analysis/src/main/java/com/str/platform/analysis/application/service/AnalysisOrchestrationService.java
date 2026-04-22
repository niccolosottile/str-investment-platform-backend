package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.mapper.AnalysisResultEntityMapper;
import com.str.platform.analysis.infrastructure.persistence.repository.JpaAnalysisResultRepository;
import com.str.platform.scraping.application.service.PropertyService;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.str.platform.shared.domain.exception.ValidationException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PropertyDataAnalysisService propertyDataAnalysisService;
    private final PropertyService propertyService;
    private final JpaAnalysisResultRepository analysisResultRepository;
    private final AnalysisResultEntityMapper analysisResultMapper;
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
        InvestmentConfiguration config = new InvestmentConfiguration(
            locationId,
            investmentType,
            budget,
            propertyType,
            goal
        );

        if (acceptsRenovation) {
            config.setAcceptsRenovation(true);
        }

        log.info("Starting investment analysis for locationId={} with budget: {}",
            locationId, config.getBudget());
        
        List<Property> properties = propertyService.getPropertiesByLocation(locationId).stream()
            .filter(property -> matchesRequestedPropertyType(property, propertyType))
            .toList();
        
        if (properties.isEmpty()) {
            log.error("No properties found for location {}", locationId);
            throw new ValidationException(
                "This market is not ready for analysis yet. We are still collecting enough live property data to generate a reliable investment outlook. Please try another location for now."
            );
        }
        
        MarketAnalysis marketAnalysis = marketAnalysisService.analyzeMarket(
            locationId,
            properties
        );
        
        if (marketAnalysis == null) {
            log.error("Market analysis returned null for location {} - insufficient scraped data", locationId);
            throw new ValidationException(
                "This market is not ready for analysis yet. We do not have enough live pricing and availability data to produce a reliable result. Please try another location for now."
            );
        }
        
        InvestmentMetrics metrics = investmentAnalysisService.calculateMetrics(
            config,
            marketAnalysis
        );
    
        Set<UUID> propertyIds = properties.stream()
            .map(Property::getId)
            .collect(Collectors.toSet());
        AnalysisDataCoverage dataCoverage = propertyDataAnalysisService.summarizeDataCoverage(propertyIds, properties.size());
        AnalysisResult.DataQuality dataQuality = 
            investmentAnalysisService.determineDataQuality(dataCoverage);
        
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
     * Check if analysis results need refresh
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
     * Evict all cached analysis results for a specific location.
     * Called when new property data becomes available for a location.
     */
    public void evictAnalysisCacheForLocation(UUID locationId) {
        try {
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
            // Log but don't fail - cache eviction is non-critical, will expire eventually
            log.error("Failed to evict analysis cache for locationId={}: {}", locationId, e.getMessage());
        }
    }

    private boolean matchesRequestedPropertyType(
            Property property,
            InvestmentConfiguration.PropertyType requestedType
    ) {
        return switch (requestedType) {
            case APARTMENT -> property.getPropertyType() == Property.PropertyType.ENTIRE_APARTMENT;
            case HOUSE -> property.getPropertyType() == Property.PropertyType.ENTIRE_HOUSE;
            case ROOM -> property.getPropertyType() == Property.PropertyType.PRIVATE_ROOM
                || property.getPropertyType() == Property.PropertyType.SHARED_ROOM;
        };
    }
}
