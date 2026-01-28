package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.mapper.AnalysisResultEntityMapper;
import com.str.platform.analysis.infrastructure.persistence.repository.JpaAnalysisResultRepository;
import com.str.platform.location.application.service.LocationService;
import com.str.platform.location.domain.model.Location;
import com.str.platform.scraping.application.service.PropertyService;
import com.str.platform.scraping.domain.model.Property;
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
    private final PropertyService propertyService;
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
        
        List<Property> properties = propertyService.getPropertiesByLocation(locationId);
        
        if (properties.isEmpty()) {
            log.error("No properties found for location {}", locationId);
            throw new IllegalStateException("Cannot perform analysis: No properties available for location " + locationId);
        }
        
        MarketAnalysis marketAnalysis = marketAnalysisService.analyzeMarket(
            locationId,
            config.getLocation(),
            properties
        );
        
        if (marketAnalysis == null) {
            log.error("Market analysis returned null for location {} - insufficient scraped data", locationId);
            throw new IllegalStateException("Cannot perform analysis: Insufficient scraped data for location " + locationId + 
                ". Please ensure scraping has been completed for this location.");
        }
        
        InvestmentMetrics metrics = investmentAnalysisService.calculateMetrics(
            config,
            marketAnalysis
        );
    
        AnalysisResult.DataQuality dataQuality = 
            investmentAnalysisService.determineDataQuality(properties.size());
        
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
}
