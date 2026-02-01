package com.str.platform.analysis.application.service;

import com.str.platform.analysis.domain.model.*;
import com.str.platform.analysis.infrastructure.persistence.entity.AnalysisResultEntity;
import com.str.platform.analysis.infrastructure.persistence.mapper.AnalysisResultEntityMapper;
import com.str.platform.analysis.infrastructure.persistence.repository.JpaAnalysisResultRepository;
import com.str.platform.scraping.application.service.PropertyService;
import com.str.platform.scraping.domain.model.Property;
import com.str.platform.scraping.domain.model.ScrapingJob;
import com.str.platform.location.domain.model.Coordinates;
import com.str.platform.shared.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisOrchestrationServiceTest {

    private static final UUID LOCATION_ID = UUID.fromString("b9353cb4-26fb-44d5-b009-4fb558fead80");
    private static final UUID ANALYSIS_ID = UUID.fromString("a1234567-89ab-cdef-0123-456789abcdef");
    private static final Money BUDGET_200K = Money.euros(200000);
    private static final Money DAILY_RATE_100 = Money.euros(100);
    private static final BigDecimal OCCUPANCY_70 = new BigDecimal("0.70");

    @Mock
    private InvestmentAnalysisService investmentAnalysisService;

    @Mock
    private MarketAnalysisService marketAnalysisService;

    @Mock
    private PropertyService propertyService;

    @Mock
    private JpaAnalysisResultRepository analysisResultRepository;

    @Mock
    private AnalysisResultEntityMapper analysisResultMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private AnalysisOrchestrationService sut;

    @Nested
    @DisplayName("Analysis Execution")
    class AnalysisExecution {

        @Test
        void shouldPerformCompleteAnalysis() {
            // Given
            givenPropertiesAvailable();
            givenValidMarketAnalysis();
            givenValidInvestmentMetrics();
            givenAnalysisCanBeSaved();

            // When
            AnalysisResult result = sut.performAnalysisForLocation(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                BUDGET_200K,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.STABLE_INCOME,
                false
            );

            // Then
            assertThat(result).isNotNull();
            verify(marketAnalysisService).analyzeMarket(eq(LOCATION_ID), any());
            verify(investmentAnalysisService).calculateMetrics(any(), any());
            verify(analysisResultRepository).save(any());
        }

        @Test
        void shouldThrowExceptionWhenNoPropertiesAvailable() {
            // Given
            when(propertyService.getPropertiesByLocation(LOCATION_ID)).thenReturn(List.of());

            // When / Then
            assertThatThrownBy(() -> sut.performAnalysisForLocation(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                BUDGET_200K,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.STABLE_INCOME,
                false
            ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No properties available");
        }

        @Test
        void shouldThrowExceptionWhenMarketAnalysisFails() {
            // Given
            givenPropertiesAvailable();
            when(marketAnalysisService.analyzeMarket(any(), any())).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> sut.performAnalysisForLocation(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                BUDGET_200K,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.STABLE_INCOME,
                false
            ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Insufficient scraped data");
        }

        @Test
        void shouldSetRenovationPreferenceWhenRequested() {
            // Given
            givenPropertiesAvailable();
            givenValidMarketAnalysis();
            givenValidInvestmentMetrics();
            givenAnalysisCanBeSaved();

            // When
            sut.performAnalysisForLocation(
                LOCATION_ID,
                InvestmentConfiguration.InvestmentType.BUY,
                BUDGET_200K,
                InvestmentConfiguration.PropertyType.APARTMENT,
                InvestmentConfiguration.InvestmentGoal.STABLE_INCOME,
                true
            );

            // Then
            verify(investmentAnalysisService).calculateMetrics(
                argThat(config -> config.isAcceptsRenovation()),
                any()
            );
        }
    }

    @Nested
    @DisplayName("Analysis Retrieval")
    class AnalysisRetrieval {

        @Test
        void shouldRetrieveExistingAnalysis() {
            // Given
            AnalysisResultEntity entity = mock(AnalysisResultEntity.class);
            AnalysisResult expectedResult = createAnalysisResult();
            
            when(analysisResultRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(entity));
            when(analysisResultMapper.toDomain(entity)).thenReturn(expectedResult);

            // When
            AnalysisResult result = sut.getAnalysis(ANALYSIS_ID);

            // Then
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        void shouldThrowExceptionWhenAnalysisNotFound() {
            // Given
            when(analysisResultRepository.findById(ANALYSIS_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> sut.getAnalysis(ANALYSIS_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("AnalysisResult");
        }
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagement {

        @Test
        void shouldEvictCacheEntriesForLocation() {
            // Given
            String pattern = "analysisResults::" + LOCATION_ID + "-*";
            Set<String> mockKeys = Set.of(
                "analysisResults::" + LOCATION_ID + "-key1",
                "analysisResults::" + LOCATION_ID + "-key2"
            );
            when(redisTemplate.keys(pattern)).thenReturn(mockKeys);
            when(redisTemplate.delete(mockKeys)).thenReturn(2L);

            // When
            sut.evictAnalysisCacheForLocation(LOCATION_ID);

            // Then
            verify(redisTemplate).keys(pattern);
            verify(redisTemplate).delete(mockKeys);
        }

        @Test
        void shouldHandleEmptyCacheGracefully() {
            // Given
            String pattern = "analysisResults::" + LOCATION_ID + "-*";
            when(redisTemplate.keys(pattern)).thenReturn(Set.of());

            // When
            sut.evictAnalysisCacheForLocation(LOCATION_ID);

            // Then
            verify(redisTemplate).keys(pattern);
            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        void shouldHandleCacheEvictionErrors() {
            // Given
            String pattern = "analysisResults::" + LOCATION_ID + "-*";
            when(redisTemplate.keys(pattern)).thenThrow(new RuntimeException("Redis error"));

            // When / Then
            assertThatCode(() -> sut.evictAnalysisCacheForLocation(LOCATION_ID))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Refresh Detection")
    class RefreshDetection {

        @Test
        void shouldDetectWhenAnalysisNeedsRefresh() {
            // Given
            AnalysisResultEntity entity = mock(AnalysisResultEntity.class);
            AnalysisResult staleResult = createStaleAnalysisResult();
            
            when(analysisResultRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(entity));
            when(analysisResultMapper.toDomain(entity)).thenReturn(staleResult);

            // When
            boolean needsRefresh = sut.needsRefresh(ANALYSIS_ID);

            // Then
            assertThat(needsRefresh).isTrue();
        }

        @Test
        void shouldReturnTrueWhenAnalysisNotFound() {
            // Given
            when(analysisResultRepository.findById(ANALYSIS_ID)).thenReturn(Optional.empty());

            // When
            boolean needsRefresh = sut.needsRefresh(ANALYSIS_ID);

            // Then
            assertThat(needsRefresh).isTrue();
        }
    }

    private void givenPropertiesAvailable() {
        List<Property> properties = List.of(
            new Property(LOCATION_ID, ScrapingJob.Platform.AIRBNB, "prop-1",
                new Coordinates(45.4642, 9.1900), Property.PropertyType.ENTIRE_APARTMENT)
        );
        when(propertyService.getPropertiesByLocation(LOCATION_ID)).thenReturn(properties);
    }

    private void givenValidMarketAnalysis() {
        MarketAnalysis marketAnalysis = new MarketAnalysis(
            25,
            DAILY_RATE_100,
            OCCUPANCY_70,
            Money.euros(1500),
            0.15,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.LOW
        );
        when(marketAnalysisService.analyzeMarket(any(), any())).thenReturn(marketAnalysis);
    }

    private void givenValidInvestmentMetrics() {
        InvestmentMetrics metrics = new InvestmentMetrics(
            Money.euros(1200),
            Money.euros(1500),
            Money.euros(1800),
            10.0,
            120,
            0.70
        );
        when(investmentAnalysisService.calculateMetrics(any(), any())).thenReturn(metrics);
        when(investmentAnalysisService.determineDataQuality(anyInt()))
            .thenReturn(AnalysisResult.DataQuality.HIGH);
    }

    private void givenAnalysisCanBeSaved() {
        AnalysisResultEntity entity = mock(AnalysisResultEntity.class);
        AnalysisResult result = createAnalysisResult();
        
        when(analysisResultMapper.toEntity(any(), any())).thenReturn(entity);
        when(analysisResultRepository.save(entity)).thenReturn(entity);
        when(analysisResultMapper.toDomain(entity)).thenReturn(result);
    }

    private AnalysisResult createAnalysisResult() {
        InvestmentConfiguration config = new InvestmentConfiguration(
            LOCATION_ID,
            InvestmentConfiguration.InvestmentType.BUY,
            BUDGET_200K,
            InvestmentConfiguration.PropertyType.APARTMENT,
            InvestmentConfiguration.InvestmentGoal.STABLE_INCOME
        );

        InvestmentMetrics metrics = new InvestmentMetrics(
            Money.euros(1200),
            Money.euros(1500),
            Money.euros(1800),
            10.0,
            120,
            0.70
        );

        MarketAnalysis marketAnalysis = new MarketAnalysis(
            25,
            DAILY_RATE_100,
            OCCUPANCY_70,
            Money.euros(1500),
            0.15,
            MarketAnalysis.GrowthTrend.STABLE,
            MarketAnalysis.CompetitionDensity.LOW
        );

        return new AnalysisResult(config, metrics, marketAnalysis, AnalysisResult.DataQuality.HIGH);
    }

    private AnalysisResult createStaleAnalysisResult() {
        AnalysisResult result = createAnalysisResult();
        result.restore(
            UUID.randomUUID(),
            java.time.LocalDateTime.now().minusHours(7),
            java.time.LocalDateTime.now().minusHours(7)
        );
        return result;
    }
}
