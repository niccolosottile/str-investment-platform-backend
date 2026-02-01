package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.MarketAnalysis;
import com.str.platform.analysis.domain.model.Money;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.location.domain.model.Coordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketAnalysisService.
 * Tests market condition analysis and competition density calculation.
 */
@ExtendWith(MockitoExtension.class)
class MarketAnalysisServiceTest {
    
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final Money DAILY_RATE_100 = Money.euros(100);
    private static final BigDecimal OCCUPANCY_70_PERCENT = new BigDecimal("0.70");
    
    @Mock
    private PropertyDataAnalysisService propertyDataAnalysisService;
    
    @Mock
    private InvestmentAnalysisService investmentAnalysisService;
    
    @InjectMocks
    private MarketAnalysisService sut;
    
    @Nested
    @DisplayName("Market Analysis Creation")
    class MarketAnalysisCreation {
        
        @Test
        void shouldAnalyzeMarketWithValidData() {
            // Given
            givenValidMarketData();
            List<Property> properties = createPropertyList(25);
            
            // When
            MarketAnalysis result = sut.analyzeMarket(LOCATION_ID, properties);
            
            // Then
            assertThat(result)
                .isNotNull()
                .satisfies(analysis -> {
                    assertThat(analysis.getTotalListings()).isEqualTo(25);
                    assertThat(analysis.getAverageDailyRate()).isEqualTo(DAILY_RATE_100);
                    assertThat(analysis.getAverageOccupancyRate()).isEqualByComparingTo(OCCUPANCY_70_PERCENT);
                    assertThat(analysis.getCompetitionDensity()).isNotNull();
                });
        }
        
        @Test
        void shouldReturnNullWhenNoPropertiesAvailable() {
            // When
            MarketAnalysis result = sut.analyzeMarket(LOCATION_ID, List.of());
            
            // Then
            assertThat(result)
                .as("Market analysis should be null when no properties exist")
                .isNull();
        }
        
        @Test
        void shouldReturnNullWhenNoPriceSampleData() {
            // Given
            when(propertyDataAnalysisService.calculateAverageDailyRate(LOCATION_ID)).thenReturn(null);
            
            // When
            MarketAnalysis result = sut.analyzeMarket(LOCATION_ID, createPropertyList(10));
            
            // Then
            assertThat(result)
                .as("Cannot analyze market without price data")
                .isNull();
        }
        
        @Test
        void shouldReturnNullWhenNoAvailabilityData() {
            // Given
            when(propertyDataAnalysisService.calculateAverageDailyRate(LOCATION_ID))
                .thenReturn(DAILY_RATE_100);
            when(propertyDataAnalysisService.calculateOccupancy(LOCATION_ID))
                .thenReturn(null);
            
            // When
            MarketAnalysis result = sut.analyzeMarket(LOCATION_ID, createPropertyList(10));
            
            // Then
            assertThat(result)
                .as("Cannot analyze market without occupancy data")
                .isNull();
        }
    }
    
    @Nested
    @DisplayName("Competition Density Classification")
    class CompetitionDensityClassification {
        
        @Test
        void shouldClassifyLowCompetitionDensity() {
            // Given
            givenValidMarketData();
            List<Property> properties = createPropertyList(5);
            
            // When
            MarketAnalysis result = sut.analyzeMarket(LOCATION_ID, properties);
            
            // Then
            assertThat(result.getCompetitionDensity())
                .as("Few properties should indicate low competition")
                .isEqualTo(MarketAnalysis.CompetitionDensity.LOW);
        }
    }
    
    private void givenValidMarketData() {
        when(propertyDataAnalysisService.calculateAverageDailyRate(LOCATION_ID))
            .thenReturn(DAILY_RATE_100);
        
        when(propertyDataAnalysisService.calculateOccupancy(LOCATION_ID))
            .thenReturn(OCCUPANCY_70_PERCENT);
        
        when(propertyDataAnalysisService.calculateSeasonalityIndex(LOCATION_ID))
            .thenReturn(0.15);
        
        when(investmentAnalysisService.calculateMonthlyRevenue(any(Money.class), anyDouble()))
            .thenReturn(Money.euros(1500));
    }
    
    private List<Property> createPropertyList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new Property(
                LOCATION_ID,
                ScrapingJob.Platform.AIRBNB,
                "prop-" + i,
                new Coordinates(45.4642, 9.1900),
                Property.PropertyType.ENTIRE_APARTMENT
            ))
            .toList();
    }
}
